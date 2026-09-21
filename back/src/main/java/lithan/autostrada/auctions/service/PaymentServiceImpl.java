package lithan.autostrada.auctions.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.config.StripeProperties;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarBidding;
import lithan.autostrada.auctions.entity.PaymentAccount;
import lithan.autostrada.auctions.entity.PaymentOrder;
import lithan.autostrada.auctions.entity.PaymentWebhookEvent;
import lithan.autostrada.auctions.identity.CurrentIdentity;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.payment.StripeAccountState;
import lithan.autostrada.auctions.payment.StripeCheckoutResult;
import lithan.autostrada.auctions.payment.StripeGateway;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;
import lithan.autostrada.auctions.repository.CarBiddingRepository;
import lithan.autostrada.auctions.repository.PaymentAccountRepository;
import lithan.autostrada.auctions.repository.PaymentOrderRepository;
import lithan.autostrada.auctions.repository.PaymentWebhookEventRepository;

@Service
public class PaymentServiceImpl implements PaymentService {

  @org.springframework.beans.factory.annotation.Autowired
  private lithan.autostrada.auctions.identity.CheckoutProfileClient checkoutProfiles;

  private final StripeGateway stripeGateway;
  private final StripeProperties properties;
  private final CurrentIdentity currentIdentity;
  private final CarBiddingRepository bidRepository;
  private final PaymentAccountRepository accountRepository;
  private final PaymentOrderRepository paymentRepository;
  private final PaymentWebhookEventRepository webhookEventRepository;

  public PaymentServiceImpl(
      StripeGateway stripeGateway,
      StripeProperties properties,
      CurrentIdentity currentIdentity,
      CarBiddingRepository bidRepository,
      PaymentAccountRepository accountRepository,
      PaymentOrderRepository paymentRepository,
      PaymentWebhookEventRepository webhookEventRepository) {
    this.stripeGateway = stripeGateway;
    this.properties = properties;
    this.currentIdentity = currentIdentity;
    this.bidRepository = bidRepository;
    this.accountRepository = accountRepository;
    this.paymentRepository = paymentRepository;
    this.webhookEventRepository = webhookEventRepository;
  }

  @Override
  public boolean isStripeEnabled() {
    return stripeGateway.isEnabled();
  }

  @Override
  public Optional<PaymentAccount> getCurrentSellerAccount() {
    return accountRepository.findByUserId(currentIdentity.requireUserId());
  }

  @Override
  @Transactional
  public String startSellerOnboarding() {
    int seller = currentIdentity.requireUserId();
    PaymentAccount account = accountRepository.findByUserId(seller).orElseGet(() -> createAccount(seller));
    return stripeGateway.createOnboardingLink(account.getProviderAccountId());
  }

  private PaymentAccount createAccount(int seller) {
    Instant now = Instant.now();
    PaymentAccount account = new PaymentAccount();
    account.setUserId(seller);
    account.setProviderAccountId(stripeGateway.createConnectedAccount(seller, checkoutProfiles.current().name()));
    account.setStatus("PENDING");
    account.setTransfersEnabled(false);
    account.setCreatedAt(now);
    account.setUpdatedAt(now);
    return accountRepository.save(account);
  }

  @Override
  @Transactional
  public PaymentAccount refreshCurrentSellerAccount() {
    PaymentAccount account = accountRepository.findByUserId(currentIdentity.requireUserId())
        .orElseThrow(ResourceNotFoundException::new);
    StripeAccountState state = stripeGateway.retrieveAccountState(account.getProviderAccountId());
    account.setTransfersEnabled(state.transfersEnabled());
    account.setStatus(state.transfersEnabled() ? "ACTIVE" : state.status());
    account.setUpdatedAt(Instant.now());
    return accountRepository.save(account);
  }

  @Override
  @Transactional
  public PaymentOrder acceptBidForPayment(int bidId) {
    CarBidding bid = bidRepository.findById(bidId).orElseThrow(ResourceNotFoundException::new);
    Car car = bid.getCar();
    if (!"ONGOING".equals(bid.getStatus()) || !"ACTIVE".equals(car.getStatus())) {
      throw new IllegalStateException("Only ongoing bids on active cars can be accepted");
    }
    PaymentAccount sellerAccount = accountRepository.findByUserId(car.getUserId())
        .orElseThrow(() -> new IllegalStateException("The seller must connect a payout account first"));
    if (!sellerAccount.isTransfersEnabled()) {
      throw new IllegalStateException("The seller payout account is not ready");
    }

    long amountMinor = Math.multiplyExact((long) bid.getBidPrice(), 100L);
    long feeMinor = Math.round(amountMinor * (properties.getPlatformFeeBasisPoints() / 10000.0));
    Instant now = Instant.now();

    PaymentOrder payment = new PaymentOrder();
    payment.setBid(bid);
    payment.setBuyerId(bid.getUserId());
    payment.setSellerId(car.getUserId());
    payment.setAmountMinor(amountMinor);
    payment.setPlatformFeeMinor(feeMinor);
    payment.setCurrency(properties.getCurrency().toLowerCase());
    payment.setStatus("PENDING_CHECKOUT");
    payment.setPurpose("AUCTION_PURCHASE");
    payment.setCreatedAt(now);
    payment.setUpdatedAt(now);

    bid.setStatus("ACCEPTED_PENDING_PAYMENT");
    car.setStatus("RESERVED");
    bidRepository.findByCarIdCar(car.getIdCar()).stream()
        .filter(other -> other.getIdBid() != bid.getIdBid() && "ONGOING".equals(other.getStatus()))
        .forEach(other -> other.setStatus("DENIED"));

    return paymentRepository.save(payment);
  }

  @Override
  @Transactional
  public String createBuyerCheckout(int paymentId) {
    PaymentOrder payment = paymentRepository.findById(paymentId).orElseThrow(ResourceNotFoundException::new);
    int buyer = currentIdentity.requireUserId();
    if (payment.getBuyerId() != buyer) {
      throw new AccessDeniedException("This payment belongs to another buyer");
    }
    if ("PAID".equals(payment.getStatus())) {
      throw new IllegalStateException("This payment is already complete");
    }
    if (payment.getCheckoutUrl() != null && "CHECKOUT_CREATED".equals(payment.getStatus())) {
      return payment.getCheckoutUrl();
    }
    if (!"PENDING_CHECKOUT".equals(payment.getStatus())) {
      throw new IllegalStateException("This payment can no longer be started");
    }

    PaymentAccount sellerAccount = accountRepository.findByUserId(payment.getSellerId())
        .filter(PaymentAccount::isTransfersEnabled)
        .orElseThrow(() -> new IllegalStateException("The seller payout account is not ready"));
    StripeCheckoutResult checkout = stripeGateway.createCheckoutSession(
        payment, sellerAccount.getProviderAccountId());
    payment.setCheckoutSessionId(checkout.sessionId());
    payment.setCheckoutUrl(checkout.checkoutUrl());
    payment.setStatus("CHECKOUT_CREATED");
    payment.setUpdatedAt(Instant.now());
    paymentRepository.save(payment);
    return checkout.checkoutUrl();
  }

  @Override
  public Page<PaymentOrder> listCurrentUserPurchases(Pageable pageable) {
    return paymentRepository.findByBuyerId(currentIdentity.requireUserId(), pageable);
  }

  @Override
  public Page<PaymentOrder> listCurrentUserSales(Pageable pageable) {
    return paymentRepository.findBySellerId(currentIdentity.requireUserId(), pageable);
  }

  @Override
  public Page<PaymentOrder> listAllPayments(Pageable pageable) {
    return paymentRepository.findAll(pageable);
  }

  @Override
  public Page<PaymentWebhookEvent> listWebhookEvents(Pageable pageable) {
    return webhookEventRepository.findAll(pageable);
  }

  @Override
  public Optional<PaymentOrder> findCurrentBuyerPaymentBySession(String sessionId) {
    int buyer = currentIdentity.requireUserId();
    return paymentRepository.findByCheckoutSessionId(sessionId)
        .filter(payment -> payment.getBuyerId() == buyer);
  }

  @Override
  @Transactional
  public void processWebhook(StripeWebhookEvent event) {
    if (webhookEventRepository.existsByProviderEventId(event.eventId())) {
      return;
    }

    if (event.checkoutSessionId() != null) {
      paymentRepository.findByCheckoutSessionId(event.checkoutSessionId())
          .ifPresent(payment -> applyPaymentEvent(payment, event));
    }

    PaymentWebhookEvent processed = new PaymentWebhookEvent();
    processed.setProviderEventId(event.eventId());
    processed.setEventType(event.eventType());
    processed.setProcessedAt(Instant.now());
    webhookEventRepository.save(processed);
  }

  private void applyPaymentEvent(PaymentOrder payment, StripeWebhookEvent event) {
    switch (event.eventType()) {
      case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> {
        if ("paid".equalsIgnoreCase(event.paymentStatus())) {
          payment.setStatus("PAID");
          payment.setPaymentIntentId(event.paymentIntentId());
          payment.setPaidAt(Instant.now());
          payment.getBid().setStatus("PAID");
          payment.getBid().getCar().setStatus("SOLD");
        }
      }
      case "checkout.session.async_payment_failed" -> releaseReservation(payment, "PAYMENT_FAILED");
      case "checkout.session.expired" -> releaseReservation(payment, "EXPIRED");
      default -> {
        return;
      }
    }
    payment.setUpdatedAt(Instant.now());
    paymentRepository.save(payment);
  }

  private void releaseReservation(PaymentOrder payment, String status) {
    if ("PAID".equals(payment.getStatus())) {
      return;
    }
    payment.setStatus(status);
    payment.getBid().setStatus(status);
    payment.getBid().getCar().setStatus("ACTIVE");
  }
}
