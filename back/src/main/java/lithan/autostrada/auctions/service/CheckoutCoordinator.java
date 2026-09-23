package lithan.autostrada.auctions.service;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;
import lithan.autostrada.auctions.identity.CheckoutProfile;
import lithan.autostrada.auctions.identity.CheckoutProfileClient;
import lithan.autostrada.auctions.identity.CurrentIdentity;
import lithan.autostrada.auctions.payment.PaymentProviderException;
import lithan.autostrada.auctions.payment.StripeCheckoutResult;
import lithan.autostrada.auctions.payment.StripeGateway;
import lithan.autostrada.auctions.repository.CheckoutAttemptRepository;
import lithan.autostrada.auctions.repository.StockHoldRepository;

@Service
public class CheckoutCoordinator {
  private final CheckoutPreparationService preparation;
  private final StripeGateway stripe;
  private final CurrentIdentity identity;
  private final CheckoutProfileClient profiles;
  private final CheckoutCrashProbe crashProbe;
  private final MeterRegistry metrics;
  private final AtomicLong reconciliationFailures = new AtomicLong();

  public CheckoutCoordinator(
      CheckoutPreparationService preparation, StripeGateway stripe,
      CurrentIdentity identity, CheckoutProfileClient profiles,
      CheckoutCrashProbe crashProbe, MeterRegistry metrics,
      CheckoutAttemptRepository attempts, StockHoldRepository holds, Clock clock) {
    this.preparation = preparation;
    this.stripe = stripe;
    this.identity = identity;
    this.profiles = profiles;
    this.crashProbe = crashProbe;
    this.metrics = metrics;
    metrics.gauge("checkout.reconciliation.failures", reconciliationFailures);
    metrics.gauge("checkout.pending.attempts", attempts,
        repository -> repository.countByStatusIn(CheckoutPreparationService.RECONCILABLE));
    metrics.gauge("checkout.pending.oldest.age.seconds", attempts,
        repository -> repository
            .findFirstByStatusInOrderByCreatedAtAsc(CheckoutPreparationService.RECONCILABLE)
            .map(attempt -> Math.max(0L,
                Duration.between(attempt.getCreatedAt(), clock.instant()).toSeconds()))
            .orElse(0L));
    metrics.gauge("checkout.stock.holds", holds,
        repository -> repository.countByStatus("ACTIVE"));
  }

  public CheckoutOutcome startStore(String requestId) {
    requireEnabled();
    int userId = identity.requireUserId();
    CheckoutProfile profile = profiles.current();
    return provide(preparation.prepareStore(userId, normalize(requestId), profile), false);
  }

  public CheckoutOutcome startDeposit(int listingId, String requestId) {
    requireEnabled();
    int userId = identity.requireUserId();
    CheckoutProfile profile = profiles.current();
    return provide(preparation.prepareDeposit(userId, listingId, normalize(requestId), profile), false);
  }

  private CheckoutOutcome provide(
      CheckoutPreparationService.PreparedCheckout prepared, boolean reconciliation) {
    if ("CHECKOUT_CREATED".equals(prepared.attempt().getStatus())) return prepared.outcome();
    if (prepared.created()) {
      crashProbe.check(CheckoutCrashProbe.Point.AFTER_LOCAL_COMMIT, prepared.attempt().getAttemptId());
    }
    try {
      StripeCheckoutResult checkout;
      if (CheckoutPreparationService.STORE.equals(prepared.attempt().getPurpose())) {
        checkout = stripe.createStoreCheckoutSession(
            prepared.order(), prepared.attempt().getCustomerEmail(), prepared.attempt().getAttemptId());
      } else {
        checkout = stripe.createListingDepositCheckoutSession(
            prepared.deposit(), prepared.attempt().getCustomerEmail(), prepared.attempt().getAttemptId());
      }
      crashProbe.check(CheckoutCrashProbe.Point.AFTER_PROVIDER_CREATE, prepared.attempt().getAttemptId());
      return preparation.complete(prepared.attempt().getAttemptId(), checkout);
    } catch (PaymentProviderException providerFailure) {
      if (reconciliation) {
        reconciliationFailures.incrementAndGet();
        metrics.counter("checkout.reconciliation.attempts", "result", "failure").increment();
      }
      return preparation.markForReconciliation(
          prepared.attempt().getAttemptId(), providerFailure.getClass().getSimpleName());
    }
  }

  @Scheduled(fixedDelayString = "${checkout.reconciliation.poll-ms:5000}")
  public void reconcile() {
    if (!stripe.isEnabled()) return;
    preparation.expireAbandoned(20);
    for (String attemptId : preparation.dueAttemptIds(20)) {
      try {
        CheckoutOutcome outcome = provide(preparation.load(attemptId), true);
        if ("CHECKOUT_CREATED".equals(outcome.status())) {
          metrics.counter("checkout.reconciliation.attempts", "result", "success").increment();
        }
      } catch (RuntimeException unexpected) {
        reconciliationFailures.incrementAndGet();
        metrics.counter("checkout.reconciliation.attempts", "result", "failure").increment();
        preparation.markForReconciliation(attemptId, unexpected.getClass().getSimpleName());
      }
    }
  }

  private void requireEnabled() {
    if (!stripe.isEnabled()) throw new IllegalStateException("Stripe sandbox checkout is not currently enabled");
  }

  private static String normalize(String requestId) {
    if (requestId == null || requestId.isBlank()) return UUID.randomUUID().toString();
    String normalized = requestId.trim();
    if (normalized.length() > 100) throw new IllegalArgumentException("Idempotency-Key is too long");
    return normalized;
  }
}
