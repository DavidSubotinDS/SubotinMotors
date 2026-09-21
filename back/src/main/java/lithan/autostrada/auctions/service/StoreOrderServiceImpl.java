package lithan.autostrada.auctions.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.config.StripeProperties;
import lithan.autostrada.auctions.entity.CartItem;
import lithan.autostrada.auctions.entity.StoreOrder;
import lithan.autostrada.auctions.entity.StoreOrderItem;
import lithan.autostrada.auctions.identity.CurrentIdentity;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.error.MissingShippingAddressException;
import lithan.autostrada.auctions.payment.StripeCheckoutResult;
import lithan.autostrada.auctions.payment.StripeGateway;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;
import lithan.autostrada.auctions.repository.CartItemRepository;
import lithan.autostrada.auctions.repository.PaymentWebhookEventRepository;
import lithan.autostrada.auctions.repository.StoreOrderRepository;
import lithan.autostrada.auctions.entity.PaymentWebhookEvent;

@Service
public class StoreOrderServiceImpl implements StoreOrderService {

  private final StoreOrderRepository orderRepository;
  private final CartItemRepository cartItemRepository;
  private final PaymentWebhookEventRepository webhookEventRepository;
  private final CurrentIdentity currentIdentity;
  @org.springframework.beans.factory.annotation.Autowired
  private lithan.autostrada.auctions.identity.CheckoutProfileClient checkoutProfiles;

  private final StripeGateway stripeGateway;
  private final StripeProperties stripeProperties;

  public StoreOrderServiceImpl(
      StoreOrderRepository orderRepository,
      CartItemRepository cartItemRepository,
      PaymentWebhookEventRepository webhookEventRepository,
      CurrentIdentity currentIdentity,
      StripeGateway stripeGateway,
      StripeProperties stripeProperties) {
    this.orderRepository = orderRepository;
    this.cartItemRepository = cartItemRepository;
    this.webhookEventRepository = webhookEventRepository;
    this.currentIdentity = currentIdentity;
    this.stripeGateway = stripeGateway;
    this.stripeProperties = stripeProperties;
  }

  @Override
  public boolean isStripeEnabled() {
    return stripeGateway.isEnabled();
  }

  @Override
  @Transactional
  public String startCheckout() {
    if (!stripeGateway.isEnabled()) {
      throw new IllegalStateException("Stripe sandbox checkout is not currently enabled");
    }
    int user = currentIdentity.requireUserId();
    var profile = checkoutProfiles.current();
    if (!profile.hasCompleteShippingAddress()) {
      throw new MissingShippingAddressException();
    }
    List<CartItem> cart = cartItemRepository.findByUserIdOrderByCreatedAtAsc(user);
    if (cart.isEmpty()) {
      throw new IllegalStateException("Your cart is empty");
    }

    Instant now = Instant.now();
    StoreOrder order = new StoreOrder();
    order.setUserId(user);
    order.setCurrency(stripeProperties.getCurrency().toLowerCase());
    order.setStatus("CREATING_CHECKOUT");
    order.setShippingName(profile.name());
    order.setShippingAddress(profile.formattedAddress());
    order.setShippingStreetAddress(profile.streetAddress().trim());
    order.setShippingCity(profile.city().trim());
    order.setShippingPostalCode(profile.postalCode().trim());
    order.setShippingCountry(profile.country().trim());
    order.setCreatedAt(now);
    order.setUpdatedAt(now);

    long totalMinor = 0;
    for (CartItem cartItem : cart) {
      if (!cartItem.getPart().isActive()) {
        throw new IllegalStateException(cartItem.getPart().getName() + " is no longer available");
      }
      if (cartItem.getQuantity() > cartItem.getPart().getStockQuantity()) {
        throw new IllegalStateException(
            "Only " + cartItem.getPart().getStockQuantity() + " units of "
                + cartItem.getPart().getName() + " remain");
      }
      cartItem.getPart().setStockQuantity(
          cartItem.getPart().getStockQuantity() - cartItem.getQuantity());
      cartItem.getPart().setUpdatedAt(now);

      StoreOrderItem orderItem = new StoreOrderItem();
      orderItem.setPart(cartItem.getPart());
      orderItem.setSku(cartItem.getPart().getSku());
      orderItem.setPartName(cartItem.getPart().getName());
      orderItem.setUnitPriceMinor(cartItem.getPart().getPriceMinor());
      orderItem.setQuantity(cartItem.getQuantity());
      order.addItem(orderItem);
      totalMinor = Math.addExact(totalMinor, orderItem.getLineTotalMinor());
    }
    order.setTotalMinor(totalMinor);
    orderRepository.saveAndFlush(order);

    StripeCheckoutResult checkout = stripeGateway.createStoreCheckoutSession(order, profile.email());
    order.setCheckoutSessionId(checkout.sessionId());
    order.setCheckoutUrl(checkout.checkoutUrl());
    order.setStatus("CHECKOUT_CREATED");
    order.setUpdatedAt(Instant.now());
    cartItemRepository.deleteByUserId(user);
    return checkout.checkoutUrl();
  }

  @Override
  public Page<StoreOrder> currentUserOrders(Pageable pageable) {
    return orderRepository.findByUserId(currentIdentity.requireUserId(), pageable);
  }

  @Override
  public Page<StoreOrder> allOrders(Pageable pageable) {
    return orderRepository.findAll(pageable);
  }

  @Override
  public StoreOrder adminOrder(int idOrder) {
    return orderRepository.findById(idOrder).orElseThrow(ResourceNotFoundException::new);
  }

  @Override
  public StoreOrder currentUserOrder(int idOrder) {
    return orderRepository.findByIdOrderAndUserId(idOrder, currentIdentity.requireUserId())
        .orElseThrow(ResourceNotFoundException::new);
  }

  @Override
  public StoreOrder currentUserOrderBySession(String sessionId) {
    StoreOrder order = orderRepository.findByCheckoutSessionId(sessionId)
        .orElseThrow(ResourceNotFoundException::new);
    if (order.getUserId() != currentIdentity.requireUserId()) {
      throw new ResourceNotFoundException();
    }
    return order;
  }

  @Override
  @Transactional
  public boolean processWebhook(StripeWebhookEvent event) {
    if (webhookEventRepository.existsByProviderEventId(event.eventId())
        || event.checkoutSessionId() == null) {
      return webhookEventRepository.existsByProviderEventId(event.eventId());
    }

    StoreOrder order = orderRepository.findByCheckoutSessionId(event.checkoutSessionId()).orElse(null);
    if (order == null) {
      return false;
    }

    switch (event.eventType()) {
      case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> {
        if ("paid".equalsIgnoreCase(event.paymentStatus())) {
          order.setStatus("PAID");
          order.setPaymentIntentId(event.paymentIntentId());
          order.setPaidAt(Instant.now());
        }
      }
      case "checkout.session.async_payment_failed" -> restoreInventory(order, "PAYMENT_FAILED");
      case "checkout.session.expired" -> restoreInventory(order, "EXPIRED");
      default -> {
        return false;
      }
    }
    order.setUpdatedAt(Instant.now());
    orderRepository.save(order);

    PaymentWebhookEvent processed = new PaymentWebhookEvent();
    processed.setProviderEventId(event.eventId());
    processed.setEventType(event.eventType());
    processed.setProcessedAt(Instant.now());
    webhookEventRepository.save(processed);
    return true;
  }

  private void restoreInventory(StoreOrder order, String status) {
    if ("PAID".equals(order.getStatus())
        || "PAYMENT_FAILED".equals(order.getStatus())
        || "EXPIRED".equals(order.getStatus())) {
      return;
    }
    order.setStatus(status);
    for (StoreOrderItem item : order.getItems()) {
      item.getPart().setStockQuantity(item.getPart().getStockQuantity() + item.getQuantity());
      item.getPart().setUpdatedAt(Instant.now());
    }
  }
}
