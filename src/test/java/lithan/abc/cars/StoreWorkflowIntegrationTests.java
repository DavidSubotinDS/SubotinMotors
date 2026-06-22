package lithan.abc.cars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarBidding;
import lithan.abc.cars.entity.CarPart;
import lithan.abc.cars.entity.StoreOrder;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.payment.StripeCheckoutResult;
import lithan.abc.cars.payment.StripeGateway;
import lithan.abc.cars.payment.StripeWebhookEvent;
import lithan.abc.cars.repository.CarBiddingRepository;
import lithan.abc.cars.repository.CarPartRepository;
import lithan.abc.cars.repository.CarRepository;
import lithan.abc.cars.repository.CartItemRepository;
import lithan.abc.cars.repository.PaymentOrderRepository;
import lithan.abc.cars.repository.PaymentWebhookEventRepository;
import lithan.abc.cars.repository.StoreOrderRepository;
import lithan.abc.cars.repository.UserRepository;
import lithan.abc.cars.service.AdminService;
import lithan.abc.cars.service.CartService;
import lithan.abc.cars.service.StoreOrderService;

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
  private AdminService adminService;

  @MockitoBean
  private StripeGateway stripeGateway;

  @BeforeEach
  void configureStripeSandbox() {
    when(stripeGateway.isEnabled()).thenReturn(true);
    when(stripeGateway.createStoreCheckoutSession(any(StoreOrder.class)))
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
  @WithMockUser(username = "user123", roles = "USER")
  void addingSameProductTwiceMergesCartRows() {
    CarPart part = partRepository.findBySkuIgnoreCase("FLT-OIL-101").orElseThrow();

    cartService.add(part.getIdPart(), 2);
    cartService.add(part.getIdPart(), 3);

    assertEquals(1, cartItemRepository.findByUserOrderByCreatedAtAsc(
        userRepository.findByUsername("user123").orElseThrow()).size());
    assertEquals(5, cartService.itemCount());
  }

  @Test
  @WithMockUser(username = "user123", roles = "USER")
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
  @WithMockUser(username = "user123", roles = "USER")
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
    car.setUser(seller);
    carRepository.save(car);

    CarBidding bid = new CarBidding();
    bid.setCar(car);
    bid.setUser(buyer);
    bid.setBidPrice(9500);
    bid.setStatus("ONGOING");
    bidRepository.save(bid);

    adminService.approveCarBid(bid.getIdBid());

    assertEquals("ACCEPTED", bid.getStatus());
    assertEquals("SOLD", car.getStatus());
    assertTrue(paymentOrderRepository.findByBid(bid).isEmpty());
  }
}
