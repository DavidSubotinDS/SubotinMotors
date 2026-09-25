package lithan.autostrada.auctions.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.StoreOrder;
import lithan.autostrada.auctions.entity.StoreOrderItem;
import lithan.autostrada.auctions.identity.CurrentIdentity;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.payment.StripeGateway;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;
import lithan.autostrada.auctions.repository.PaymentWebhookEventRepository;
import lithan.autostrada.auctions.repository.StoreOrderRepository;
import lithan.autostrada.auctions.repository.CarPartRepository;
import lithan.autostrada.auctions.entity.PaymentWebhookEvent;

@Service
public class StoreOrderServiceImpl implements StoreOrderService {

  private final StoreOrderRepository orderRepository;
  private final PaymentWebhookEventRepository webhookEventRepository;
  private final CurrentIdentity currentIdentity;

  private final StripeGateway stripeGateway;
  private final CheckoutCoordinator checkoutCoordinator;
  private final CheckoutPreparationService checkoutPreparation;
  private final CarPartRepository partRepository;
  private final Clock clock;

  public StoreOrderServiceImpl(
      StoreOrderRepository orderRepository,
      PaymentWebhookEventRepository webhookEventRepository,
      CurrentIdentity currentIdentity,
      StripeGateway stripeGateway,
      CheckoutCoordinator checkoutCoordinator,
      CheckoutPreparationService checkoutPreparation,
      CarPartRepository partRepository,
      Clock clock) {
    this.orderRepository = orderRepository;
    this.webhookEventRepository = webhookEventRepository;
    this.currentIdentity = currentIdentity;
    this.stripeGateway = stripeGateway;
    this.checkoutCoordinator = checkoutCoordinator;
    this.checkoutPreparation = checkoutPreparation;
    this.partRepository = partRepository;
    this.clock = clock;
  }

  @Override
  public boolean isStripeEnabled() {
    return stripeGateway.isEnabled();
  }

  @Override
  public CheckoutOutcome startCheckout(String requestId) {
    return checkoutCoordinator.startStore(requestId);
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
    StoreOrder order = orderRepository.findByCheckoutSessionId(sessionId).orElseGet(()->orderRepository
        .findByCheckoutAttemptId(stripeGateway.findAttemptByProviderSession(sessionId)).orElseThrow(ResourceNotFoundException::new));
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
        if ("paid".equalsIgnoreCase(event.paymentStatus()) && !order.getStatus().startsWith("PAID")) {
          order.setStatus(reclaimInventoryForLatePayment(order) ? "PAID" : "PAID_STOCK_CONFLICT");
          order.setPaymentIntentId(event.paymentIntentId());
          order.setPaidAt(clock.instant());
        }
      }
      case "checkout.session.async_payment_failed" -> restoreInventory(order, "PAYMENT_FAILED");
      case "checkout.session.expired" -> restoreInventory(order, "EXPIRED");
      default -> {
        return false;
      }
    }
    order.setUpdatedAt(clock.instant());
    orderRepository.save(order);
    checkoutPreparation.recordTerminal(
        order.getCheckoutAttemptId(), order.getStatus(), order.getPaymentIntentId());

    PaymentWebhookEvent processed = new PaymentWebhookEvent();
    processed.setProviderEventId(event.eventId());
    processed.setEventType(event.eventType());
    processed.setProcessedAt(clock.instant());
    webhookEventRepository.save(processed);
    return true;
  }

  @Transactional
  public void processPaymentResult(String attemptId,String status,String paymentIntentId) {
    StoreOrder order=orderRepository.findByCheckoutAttemptId(attemptId).orElseThrow(ResourceNotFoundException::new);
    if("SUCCEEDED".equals(status)&&!order.getStatus().startsWith("PAID")){
      order.setStatus(reclaimInventoryForLatePayment(order)?"PAID":"PAID_STOCK_CONFLICT");order.setPaymentIntentId(paymentIntentId);order.setPaidAt(clock.instant());
    } else if("FAILED".equals(status)) restoreInventory(order,"PAYMENT_FAILED");
    else if("EXPIRED".equals(status)) restoreInventory(order,"EXPIRED");
    order.setUpdatedAt(clock.instant());orderRepository.save(order);checkoutPreparation.recordTerminal(attemptId,order.getStatus(),order.getPaymentIntentId());
  }

  private void restoreInventory(StoreOrder order, String status) {
    if (order.getStatus().startsWith("PAID")
        || "PAYMENT_FAILED".equals(order.getStatus())
        || "EXPIRED".equals(order.getStatus())) {
      return;
    }
    order.setStatus(status);
    for (StoreOrderItem item : order.getItems()) {
      item.getPart().setStockQuantity(item.getPart().getStockQuantity() + item.getQuantity());
      item.getPart().setUpdatedAt(clock.instant());
    }
  }

  private boolean reclaimInventoryForLatePayment(StoreOrder order) {
    if (!"EXPIRED".equals(order.getStatus()) && !"PAYMENT_FAILED".equals(order.getStatus())) {
      return true;
    }
    var locked = partRepository.findAllByIdForUpdate(order.getItems().stream()
        .map(item -> item.getPart().getIdPart()).sorted().toList());
    var quantities = order.getItems().stream().collect(java.util.stream.Collectors.toMap(
        item -> item.getPart().getIdPart(), StoreOrderItem::getQuantity));
    if (locked.stream().anyMatch(part -> part.getStockQuantity() < quantities.get(part.getIdPart()))) {
      return false;
    }
    Instant now = clock.instant();
    locked.forEach(part -> {
      part.setStockQuantity(part.getStockQuantity() - quantities.get(part.getIdPart()));
      part.setUpdatedAt(now);
    });
    return true;
  }
}
