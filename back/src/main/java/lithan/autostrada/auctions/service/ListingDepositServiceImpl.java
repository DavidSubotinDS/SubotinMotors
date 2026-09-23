package lithan.autostrada.auctions.service;

import java.time.Clock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.CarListingStatus;
import lithan.autostrada.auctions.entity.ListingDeposit;
import lithan.autostrada.auctions.entity.PaymentWebhookEvent;
import lithan.autostrada.auctions.identity.CurrentIdentity;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.payment.StripeGateway;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;
import lithan.autostrada.auctions.repository.ListingDepositRepository;
import lithan.autostrada.auctions.repository.PaymentWebhookEventRepository;

@Service
public class ListingDepositServiceImpl implements ListingDepositService {

  private final ListingDepositRepository depositRepository;
  private final PaymentWebhookEventRepository webhookEventRepository;
  private final CurrentIdentity currentIdentity;

  private final StripeGateway stripeGateway;
  private final CheckoutCoordinator checkoutCoordinator;
  private final CheckoutPreparationService checkoutPreparation;
  private final Clock clock;

  public ListingDepositServiceImpl(
      ListingDepositRepository depositRepository,
      PaymentWebhookEventRepository webhookEventRepository,
      CurrentIdentity currentIdentity,
      StripeGateway stripeGateway,
      CheckoutCoordinator checkoutCoordinator,
      CheckoutPreparationService checkoutPreparation,
      Clock clock) {
    this.depositRepository = depositRepository;
    this.webhookEventRepository = webhookEventRepository;
    this.currentIdentity = currentIdentity;
    this.stripeGateway = stripeGateway;
    this.checkoutCoordinator = checkoutCoordinator;
    this.checkoutPreparation = checkoutPreparation;
    this.clock = clock;
  }

  @Override
  public boolean isStripeEnabled() {
    return stripeGateway.isEnabled();
  }

  @Override
  public CheckoutOutcome startCheckout(int listingId, String requestId) {
    return checkoutCoordinator.startDeposit(listingId, requestId);
  }

  @Override
  public Page<ListingDeposit> currentUserDeposits(Pageable pageable) {
    return depositRepository.findByBuyerId(currentIdentity.requireUserId(), pageable);
  }

  @Override
  public ListingDeposit currentUserDepositBySession(String sessionId) {
    ListingDeposit deposit = depositRepository.findByCheckoutSessionId(sessionId)
        .orElseThrow(ResourceNotFoundException::new);
    if (deposit.getBuyerId() != currentIdentity.requireUserId()) {
      throw new ResourceNotFoundException();
    }
    return deposit;
  }

  @Override
  @Transactional
  public boolean processWebhook(StripeWebhookEvent event) {
    if (webhookEventRepository.existsByProviderEventId(event.eventId())) {
      return true;
    }
    if (event.checkoutSessionId() == null) {
      return false;
    }
    ListingDeposit deposit = depositRepository.findByCheckoutSessionId(
        event.checkoutSessionId()).orElse(null);
    if (deposit == null) {
      return false;
    }

    switch (event.eventType()) {
      case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> {
        if ("paid".equalsIgnoreCase(event.paymentStatus()) && !deposit.getStatus().startsWith("PAID")) {
          deposit.setStatus(reclaimLateReservation(deposit) ? "PAID" : "PAID_RESERVATION_CONFLICT");
          deposit.setPaymentIntentId(event.paymentIntentId());
          deposit.setPaidAt(clock.instant());
        }
      }
      case "checkout.session.async_payment_failed" -> release(deposit, "PAYMENT_FAILED");
      case "checkout.session.expired" -> release(deposit, "EXPIRED");
      default -> {
        return false;
      }
    }
    deposit.setUpdatedAt(clock.instant());
    depositRepository.save(deposit);
    checkoutPreparation.recordTerminal(
        deposit.getCheckoutAttemptId(), deposit.getStatus(), deposit.getPaymentIntentId());

    PaymentWebhookEvent processed = new PaymentWebhookEvent();
    processed.setProviderEventId(event.eventId());
    processed.setEventType(event.eventType());
    processed.setProcessedAt(clock.instant());
    webhookEventRepository.save(processed);
    return true;
  }

  private void release(ListingDeposit deposit, String status) {
    if (deposit.getStatus().startsWith("PAID")
        || "PAYMENT_FAILED".equals(deposit.getStatus())
        || "EXPIRED".equals(deposit.getStatus())) {
      return;
    }
    deposit.setStatus(status);
    if (deposit.getListing().getStatus() == CarListingStatus.RESERVED) {
      deposit.getListing().setStatus(CarListingStatus.ACTIVE);
      deposit.getListing().setUpdatedAt(clock.instant());
    }
  }

  private boolean reclaimLateReservation(ListingDeposit deposit) {
    if (!"EXPIRED".equals(deposit.getStatus())
        && !"PAYMENT_FAILED".equals(deposit.getStatus())) return true;
    if (deposit.getListing().getStatus() != CarListingStatus.ACTIVE) return false;
    deposit.getListing().setStatus(CarListingStatus.RESERVED);
    deposit.getListing().setUpdatedAt(clock.instant());
    return true;
  }
}
