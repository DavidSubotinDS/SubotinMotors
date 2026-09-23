package lithan.autostrada.auctions.service;

import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lithan.autostrada.auctions.entity.CheckoutWebhookInbox;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;
import lithan.autostrada.auctions.repository.CheckoutWebhookInboxRepository;
import lithan.autostrada.auctions.repository.PaymentOrderRepository;

@Service
public class CheckoutWebhookInboxService {
  private final CheckoutWebhookInboxRepository inbox;
  private final PaymentOrderRepository paymentOrders;
  private final StoreOrderService storeOrders;
  private final ListingDepositService deposits;
  private final PaymentService payments;
  private final Clock clock;

  public CheckoutWebhookInboxService(
      CheckoutWebhookInboxRepository inbox, PaymentOrderRepository paymentOrders,
      StoreOrderService storeOrders, ListingDepositService deposits, PaymentService payments,
      Clock clock) {
    this.inbox = inbox;
    this.paymentOrders = paymentOrders;
    this.storeOrders = storeOrders;
    this.deposits = deposits;
    this.payments = payments;
    this.clock = clock;
  }

  @Transactional
  public void receive(StripeWebhookEvent event) {
    CheckoutWebhookInbox entry = inbox.findByProviderEventId(event.eventId()).orElse(null);
    if (entry == null) {
      Instant now = clock.instant();
      entry = new CheckoutWebhookInbox();
      entry.setProviderEventId(event.eventId());
      entry.setEventType(event.eventType());
      entry.setCheckoutSessionId(event.checkoutSessionId());
      entry.setPaymentIntentId(event.paymentIntentId());
      entry.setPaymentStatus(event.paymentStatus());
      entry.setStatus("PENDING");
      entry.setDeliveryCount(1);
      entry.setReceivedAt(now);
      entry.setUpdatedAt(now);
      entry = inbox.saveAndFlush(entry);
    } else {
      entry.setDeliveryCount(entry.getDeliveryCount() + 1);
      entry.setUpdatedAt(clock.instant());
    }
    dispatch(entry);
  }

  @Scheduled(fixedDelayString = "${checkout.webhook-inbox.poll-ms:2000}")
  @Transactional
  public void retryPending() {
    inbox.findByStatusOrderByReceivedAtAsc("PENDING", PageRequest.of(0, 50))
        .forEach(this::dispatch);
  }

  private void dispatch(CheckoutWebhookInbox entry) {
    if ("PROCESSED".equals(entry.getStatus())) return;
    StripeWebhookEvent event = new StripeWebhookEvent(
        entry.getProviderEventId(), entry.getEventType(), entry.getCheckoutSessionId(),
        entry.getPaymentIntentId(), entry.getPaymentStatus());
    try {
      boolean handled = storeOrders.processWebhook(event) || deposits.processWebhook(event);
      if (!handled && event.checkoutSessionId() != null
          && paymentOrders.findByCheckoutSessionId(event.checkoutSessionId()).isPresent()) {
        payments.processWebhook(event);
        handled = true;
      }
      if (event.checkoutSessionId() == null) handled = true;
      entry.setUpdatedAt(clock.instant());
      if (handled) {
        entry.setStatus("PROCESSED");
        entry.setProcessedAt(clock.instant());
      }
      inbox.save(entry);
    } catch (RuntimeException transientFailure) {
      entry.setUpdatedAt(clock.instant());
      inbox.save(entry);
    }
  }
}
