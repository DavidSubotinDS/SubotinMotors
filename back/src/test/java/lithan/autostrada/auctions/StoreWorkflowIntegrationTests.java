package lithan.autostrada.auctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarBidding;
import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.StoreOrder;
import fixtures.identity.entity.UserAccount;
import lithan.autostrada.auctions.payment.StripeCheckoutResult;
import lithan.autostrada.auctions.payment.StripeGateway;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;
import lithan.autostrada.auctions.repository.CarBiddingRepository;
import lithan.autostrada.auctions.repository.CarPartRepository;
import lithan.autostrada.auctions.repository.CarRepository;
import lithan.autostrada.auctions.repository.CartItemRepository;
import lithan.autostrada.auctions.repository.PaymentOrderRepository;
import lithan.autostrada.auctions.repository.PaymentWebhookEventRepository;
import lithan.autostrada.auctions.repository.StoreOrderRepository;
import lithan.autostrada.auctions.repository.CheckoutAttemptRepository;
import lithan.autostrada.auctions.repository.CheckoutWebhookInboxRepository;
import lithan.autostrada.auctions.repository.StockHoldRepository;
import fixtures.identity.repository.UserRepository;
import lithan.autostrada.auctions.service.MarketplaceAdminService;
import lithan.autostrada.auctions.service.CartService;
import lithan.autostrada.auctions.service.StoreOrderService;
import lithan.autostrada.auctions.service.CheckoutCrashProbe;
import lithan.autostrada.auctions.service.CheckoutWebhookInboxService;
import lithan.autostrada.auctions.payment.PaymentProviderException;

@org.springframework.context.annotation.Import(BusinessIdentityFixtures.class)
@SpringBootTest
@Transactional
class StoreWorkflowIntegrationTests {

  @Autowired
  private CarPartRepository partRepository;

  @Autowired
  private CartItemRepository cartItemRepository;

  @Autowired
  private StoreOrderRepository orderRepository;

  @Autowired
  private PaymentWebhookEventRepository webhookEventRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CarRepository carRepository;

  @Autowired
  private CarBiddingRepository bidRepository;

  @Autowired
  private PaymentOrderRepository paymentOrderRepository;

  @Autowired
  private CartService cartService;

  @Autowired
  private StoreOrderService orderService;

  @Autowired
  private CheckoutAttemptRepository attemptRepository;

  @Autowired
  private StockHoldRepository stockHoldRepository;

  @Autowired
  private CheckoutWebhookInboxRepository checkoutWebhookInboxRepository;

  @Autowired
  private CheckoutWebhookInboxService checkoutWebhookInboxService;

  @Autowired
  private MarketplaceAdminService adminService;

  @MockitoBean
  private StripeGateway stripeGateway;

  @MockitoBean
  private CheckoutCrashProbe crashProbe;

  @BeforeEach
  void configureStripeSandbox() {
    when(stripeGateway.isEnabled()).thenReturn(true);
    when(stripeGateway.createStoreCheckoutSession(any(StoreOrder.class), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
        .thenAnswer(invocation -> {
          StoreOrder order = invocation.getArgument(0);
          return new StripeCheckoutResult(
              "cs_store_" + order.getIdOrder(),
              "https://checkout.stripe.test/store/" + order.getIdOrder());
        });
  }

  @Test
  void demoCatalogContainsActiveProductsAcrossCategories() {
    assertTrue(partRepository.count() >= 10);
    assertTrue(partRepository.findActiveCategories().size() >= 5);
    assertTrue(partRepository.findBySkuIgnoreCase("BRK-PAD-001").orElseThrow().isActive());
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void addingSameProductTwiceMergesCartRows() {
    CarPart part = partRepository.findBySkuIgnoreCase("FLT-OIL-101").orElseThrow();

    cartService.add(part.getIdPart(), 2);
    cartService.add(part.getIdPart(), 3);

    assertEquals(1, cartItemRepository.findByUserIdOrderByCreatedAtAsc(userRepository.findByUsername("user123").orElseThrow().getIdUser()).size());
    assertEquals(5, cartService.itemCount());
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void checkoutReservesInventoryAndPaidWebhookFinalizesOrder() {
    CarPart part = partRepository.findBySkuIgnoreCase("LGT-H7-PLUS").orElseThrow();
    int originalStock = part.getStockQuantity();
    cartService.add(part.getIdPart(), 2);

    String checkoutUrl = orderService.startCheckout();
    StoreOrder order = orderRepository.findAll().stream()
        .max((left, right) -> Integer.compare(left.getIdOrder(), right.getIdOrder()))
        .orElseThrow();

    assertTrue(checkoutUrl.startsWith("https://checkout.stripe.test/"));
    assertEquals("CHECKOUT_CREATED", order.getStatus());
    assertEquals(part.getPriceMinor() * 2, order.getTotalMinor());
    assertEquals(originalStock - 2, part.getStockQuantity());
    assertEquals(0, cartService.itemCount());

    boolean handled = orderService.processWebhook(new StripeWebhookEvent(
        "evt_store_paid",
        "checkout.session.completed",
        order.getCheckoutSessionId(),
        "pi_store_paid",
        "paid"));

    assertTrue(handled);
    assertEquals("PAID", order.getStatus());
    assertEquals("pi_store_paid", order.getPaymentIntentId());
    assertNotNull(order.getPaidAt());
    assertTrue(webhookEventRepository.existsByProviderEventId("evt_store_paid"));

    Instant paidAt = order.getPaidAt();
    assertTrue(orderService.processWebhook(new StripeWebhookEvent(
        "evt_store_paid",
        "checkout.session.completed",
        order.getCheckoutSessionId(),
        "pi_store_paid",
        "paid")));
    assertEquals(paidAt, order.getPaidAt());
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void expiredCheckoutRestoresReservedInventory() {
    CarPart part = partRepository.findBySkuIgnoreCase("WPR-650-400").orElseThrow();
    int originalStock = part.getStockQuantity();
    cartService.add(part.getIdPart(), 1);
    orderService.startCheckout();
    StoreOrder order = orderRepository.findAll().stream()
        .max((left, right) -> Integer.compare(left.getIdOrder(), right.getIdOrder()))
        .orElseThrow();

    orderService.processWebhook(new StripeWebhookEvent(
        "evt_store_expired",
        "checkout.session.expired",
        order.getCheckoutSessionId(),
        null,
        "unpaid"));

    assertEquals("EXPIRED", order.getStatus());
    assertEquals(originalStock, part.getStockQuantity());
    assertEquals("RELEASED", stockHoldRepository.findByAttemptId(
        order.getCheckoutAttemptId()).get(0).getStatus());
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void unpaidCompletionRemainsPendingAndLatePaidOutcomeReclaimsInventory() {
    CarPart part = partRepository.findBySkuIgnoreCase("TIR-20555R16").orElseThrow();
    int originalStock = part.getStockQuantity();
    cartService.add(part.getIdPart(), 1);
    orderService.startCheckout("late-payment-request");
    StoreOrder order = orderRepository.findByCheckoutAttemptId(
        attemptRepository.findAll().stream()
            .filter(attempt -> "late-payment-request".equals(attempt.getClientRequestId()))
            .findFirst().orElseThrow().getAttemptId()).orElseThrow();

    orderService.processWebhook(new StripeWebhookEvent(
        "evt_unpaid_complete", "checkout.session.completed",
        order.getCheckoutSessionId(), null, "unpaid"));
    assertEquals("CHECKOUT_CREATED", order.getStatus());

    orderService.processWebhook(new StripeWebhookEvent(
        "evt_expire_before_late", "checkout.session.expired",
        order.getCheckoutSessionId(), null, "unpaid"));
    assertEquals(originalStock, part.getStockQuantity());

    orderService.processWebhook(new StripeWebhookEvent(
        "evt_late_paid", "checkout.session.async_payment_succeeded",
        order.getCheckoutSessionId(), "pi_late", "paid"));
    assertEquals("PAID", order.getStatus());
    assertEquals(originalStock - 1, part.getStockQuantity());
    assertEquals("CONSUMED", stockHoldRepository.findByAttemptId(
        order.getCheckoutAttemptId()).get(0).getStatus());
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void latePaidStockConflictCannotBeDowngradedOrReleaseStockAgain() {
    CarPart part = partRepository.findBySkuIgnoreCase("TIR-20555R16").orElseThrow();
    part.setStockQuantity(1);
    partRepository.saveAndFlush(part);
    cartService.add(part.getIdPart(), 1);
    var checkout = orderService.startCheckout("late-stock-conflict");
    StoreOrder order = orderRepository.findByCheckoutAttemptId(checkout.attemptId()).orElseThrow();

    orderService.processWebhook(new StripeWebhookEvent(
        "evt_conflict_expired", "checkout.session.expired",
        order.getCheckoutSessionId(), null, "unpaid"));
    part.setStockQuantity(0); // another committed checkout consumed the released unit
    partRepository.saveAndFlush(part);

    orderService.processWebhook(new StripeWebhookEvent(
        "evt_conflict_paid", "checkout.session.async_payment_succeeded",
        order.getCheckoutSessionId(), "pi_conflict", "paid"));
    orderService.processWebhook(new StripeWebhookEvent(
        "evt_conflict_late_expiry", "checkout.session.expired",
        order.getCheckoutSessionId(), null, "unpaid"));
    orderService.processWebhook(new StripeWebhookEvent(
        "evt_conflict_duplicate_paid_outcome", "checkout.session.async_payment_succeeded",
        order.getCheckoutSessionId(), "pi_conflict", "paid"));

    assertEquals("PAID_STOCK_CONFLICT", order.getStatus());
    assertEquals("PAID_STOCK_CONFLICT", attemptRepository.findById(checkout.attemptId())
        .orElseThrow().getStatus());
    assertEquals(0, part.getStockQuantity());
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void duplicateCheckoutRequestReturnsOneDurableAttemptAndProviderSession() {
    CarPart part = partRepository.findBySkuIgnoreCase("SPK-IRIDIUM-4").orElseThrow();
    cartService.add(part.getIdPart(), 1);

    var first = orderService.startCheckout("same-browser-request");
    var duplicate = orderService.startCheckout("same-browser-request");

    assertEquals(first.attemptId(), duplicate.attemptId());
    assertEquals(first.checkoutUrl(), duplicate.checkoutUrl());
    assertEquals("CHECKOUT_CREATED", duplicate.status());
    assertEquals(1, attemptRepository.findAll().stream()
        .filter(attempt -> "same-browser-request".equals(attempt.getClientRequestId())).count());
    verify(stripeGateway, times(1)).createStoreCheckoutSession(
        any(StoreOrder.class), anyString(), org.mockito.ArgumentMatchers.eq(first.attemptId()));
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void reusingCheckoutKeyForDifferentCartIsRejected() {
    CarPart firstPart = partRepository.findBySkuIgnoreCase("SPK-IRIDIUM-4").orElseThrow();
    CarPart secondPart = partRepository.findBySkuIgnoreCase("EMG-ROAD-KIT").orElseThrow();
    cartService.add(firstPart.getIdPart(), 1);
    orderService.startCheckout("reused-for-different-cart");
    cartService.add(secondPart.getIdPart(), 1);

    var failure = org.junit.jupiter.api.Assertions.assertThrows(
        IllegalStateException.class,
        () -> orderService.startCheckout("reused-for-different-cart"));

    assertTrue(failure.getMessage().contains("already used"));
    assertEquals(1, attemptRepository.findAll().stream()
        .filter(attempt -> "reused-for-different-cart".equals(attempt.getClientRequestId())).count());
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void earlyVerifiedWebhookWaitsDurablyUntilTheCheckoutSessionIsLinked() {
    CarPart part = partRepository.findBySkuIgnoreCase("FLT-OIL-101").orElseThrow();
    cartService.add(part.getIdPart(), 1);
    var outcome = orderService.startCheckout("early-webhook-link");
    StoreOrder order = orderRepository.findByCheckoutAttemptId(outcome.attemptId()).orElseThrow();
    String linkedSession = "cs_early_arrival";
    order.setCheckoutSessionId(null);
    orderRepository.saveAndFlush(order);

    checkoutWebhookInboxService.receive(new StripeWebhookEvent(
        "evt_early_arrival", "checkout.session.completed", linkedSession,
        "pi_early_arrival", "paid"));
    assertEquals("PENDING", checkoutWebhookInboxRepository
        .findByProviderEventId("evt_early_arrival").orElseThrow().getStatus());

    order.setCheckoutSessionId(linkedSession);
    orderRepository.saveAndFlush(order);
    checkoutWebhookInboxService.retryPending();

    assertEquals("PROCESSED", checkoutWebhookInboxRepository
        .findByProviderEventId("evt_early_arrival").orElseThrow().getStatus());
    assertEquals("PAID", order.getStatus());
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void providerFailureReturnsPendingAndExplicitRetryResumesSameAttempt() {
    CarPart part = partRepository.findBySkuIgnoreCase("EMG-ROAD-KIT").orElseThrow();
    cartService.add(part.getIdPart(), 1);
    when(stripeGateway.createStoreCheckoutSession(any(StoreOrder.class), anyString(), anyString()))
        .thenThrow(new PaymentProviderException("simulated timeout"))
        .thenAnswer(invocation -> {
          StoreOrder order = invocation.getArgument(0);
          return new StripeCheckoutResult("cs_retry_" + order.getIdOrder(),
              "https://checkout.stripe.test/retry/" + order.getIdOrder());
        });

    var pending = orderService.startCheckout("retry-browser-request");
    var recovered = orderService.startCheckout("retry-browser-request");

    assertEquals("RECONCILE_REQUIRED", pending.status());
    assertTrue(pending.retryable());
    assertEquals(pending.attemptId(), recovered.attemptId());
    assertEquals("CHECKOUT_CREATED", recovered.status());
    verify(stripeGateway, times(2)).createStoreCheckoutSession(
        any(StoreOrder.class), anyString(), org.mockito.ArgumentMatchers.eq(pending.attemptId()));
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void crashAfterProviderCreationIsRecoveredWithTheSameIdempotencyKey() {
    CarPart part = partRepository.findBySkuIgnoreCase("BAT-AGM-070").orElseThrow();
    cartService.add(part.getIdPart(), 1);
    doThrow(new SimulatedCrash()).doNothing().when(crashProbe)
        .check(org.mockito.ArgumentMatchers.eq(CheckoutCrashProbe.Point.AFTER_PROVIDER_CREATE), anyString());

    org.junit.jupiter.api.Assertions.assertThrows(
        SimulatedCrash.class, () -> orderService.startCheckout("crash-browser-request"));
    var recovered = orderService.startCheckout("crash-browser-request");

    assertEquals("CHECKOUT_CREATED", recovered.status());
    verify(stripeGateway, times(2)).createStoreCheckoutSession(
        any(StoreOrder.class), anyString(), org.mockito.ArgumentMatchers.eq(recovered.attemptId()));
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void crashAfterLocalCommitResumesWithoutCreatingAnotherOrder() {
    CarPart part = partRepository.findBySkuIgnoreCase("OIL-5W30-5L").orElseThrow();
    cartService.add(part.getIdPart(), 1);
    doThrow(new SimulatedCrash()).doNothing().when(crashProbe)
        .check(org.mockito.ArgumentMatchers.eq(CheckoutCrashProbe.Point.AFTER_LOCAL_COMMIT), anyString());

    org.junit.jupiter.api.Assertions.assertThrows(
        SimulatedCrash.class, () -> orderService.startCheckout("local-commit-crash"));
    long ordersAfterCrash = orderRepository.count();
    var recovered = orderService.startCheckout("local-commit-crash");

    assertEquals("CHECKOUT_CREATED", recovered.status());
    assertEquals(ordersAfterCrash, orderRepository.count());
    verify(stripeGateway, times(1)).createStoreCheckoutSession(
        any(StoreOrder.class), anyString(), org.mockito.ArgumentMatchers.eq(recovered.attemptId()));
  }

  private static final class SimulatedCrash extends RuntimeException {
  }

  @Test
  void auctionWinnerNoLongerRequiresStripePayoutAccount() {
    UserAccount seller = userRepository.findByUsername("admin123").orElseThrow();
    UserAccount buyer = userRepository.findByUsername("user123").orElseThrow();

    Car car = new Car();
    car.setMake("Auction");
    car.setModel("No Connect");
    car.setYear("2025");
    car.setPrice(9000);
    car.setStatus("ACTIVE");
    car.setUserId(seller.getIdUser());
    carRepository.save(car);

    CarBidding bid = new CarBidding();
    bid.setCar(car);
    bid.setUserId(buyer.getIdUser());
    bid.setBidPrice(9500);
    bid.setStatus("ONGOING");
    bidRepository.save(bid);

    adminService.approveCarBid(bid.getIdBid());

    assertEquals("ACCEPTED", bid.getStatus());
    assertEquals("SOLD", car.getStatus());
    assertTrue(paymentOrderRepository.findByBid(bid).isEmpty());
  }
}
