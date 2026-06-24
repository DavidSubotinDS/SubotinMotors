package lithan.autostrada.auctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarBidding;
import lithan.autostrada.auctions.entity.PaymentAccount;
import lithan.autostrada.auctions.entity.PaymentOrder;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;
import lithan.autostrada.auctions.repository.CarBiddingRepository;
import lithan.autostrada.auctions.repository.CarRepository;
import lithan.autostrada.auctions.repository.PaymentAccountRepository;
import lithan.autostrada.auctions.repository.PaymentOrderRepository;
import lithan.autostrada.auctions.repository.PaymentWebhookEventRepository;
import lithan.autostrada.auctions.repository.UserRepository;
import lithan.autostrada.auctions.service.PaymentService;

@SpringBootTest
@Transactional
class PaymentWorkflowIntegrationTests {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CarRepository carRepository;

  @Autowired
  private CarBiddingRepository bidRepository;

  @Autowired
  private PaymentAccountRepository accountRepository;

  @Autowired
  private PaymentOrderRepository paymentRepository;

  @Autowired
  private PaymentWebhookEventRepository webhookEventRepository;

  @Autowired
  private PaymentService paymentService;

  @Test
  void acceptingBidReservesCarUntilPaymentWebhookCompletesSale() {
    UserAccount seller = userRepository.findByUsername("admin123").orElseThrow();
    UserAccount buyer = userRepository.findByUsername("user123").orElseThrow();

    PaymentAccount account = new PaymentAccount();
    account.setUser(seller);
    account.setProviderAccountId("acct_test_seller");
    account.setStatus("ACTIVE");
    account.setTransfersEnabled(true);
    account.setCreatedAt(Instant.now());
    account.setUpdatedAt(Instant.now());
    accountRepository.save(account);

    Car car = new Car();
    car.setMake("Payment");
    car.setModel("Lifecycle");
    car.setYear("2025");
    car.setPrice(10000);
    car.setStatus("ACTIVE");
    car.setUser(seller);
    carRepository.save(car);

    CarBidding bid = new CarBidding();
    bid.setCar(car);
    bid.setUser(buyer);
    bid.setBidPrice(12000);
    bid.setStatus("ONGOING");
    bidRepository.save(bid);

    PaymentOrder payment = paymentService.acceptBidForPayment(bid.getIdBid());

    assertEquals("RESERVED", car.getStatus());
    assertEquals("ACCEPTED_PENDING_PAYMENT", bid.getStatus());
    assertEquals("PENDING_CHECKOUT", payment.getStatus());
    assertEquals(1_200_000L, payment.getAmountMinor());
    assertEquals(30_000L, payment.getPlatformFeeMinor());

    payment.setCheckoutSessionId("cs_test_paid");
    payment.setCheckoutUrl("https://checkout.stripe.test/session");
    payment.setStatus("CHECKOUT_CREATED");
    paymentRepository.save(payment);

    paymentService.processWebhook(new StripeWebhookEvent(
        "evt_test_paid",
        "checkout.session.completed",
        "cs_test_paid",
        "pi_test_paid",
        "paid"));

    assertEquals("PAID", payment.getStatus());
    assertEquals("PAID", bid.getStatus());
    assertEquals("SOLD", car.getStatus());
    assertEquals("pi_test_paid", payment.getPaymentIntentId());
    assertNotNull(payment.getPaidAt());
    assertTrue(webhookEventRepository.existsByProviderEventId("evt_test_paid"));
  }

  @Test
  void duplicateWebhookIsIdempotent() {
    UserAccount seller = userRepository.findByUsername("admin123").orElseThrow();
    UserAccount buyer = userRepository.findByUsername("user123").orElseThrow();

    PaymentAccount account = new PaymentAccount();
    account.setUser(seller);
    account.setProviderAccountId("acct_test_idempotent");
    account.setStatus("ACTIVE");
    account.setTransfersEnabled(true);
    account.setCreatedAt(Instant.now());
    account.setUpdatedAt(Instant.now());
    accountRepository.save(account);

    Car car = new Car();
    car.setMake("Webhook");
    car.setModel("Idempotency");
    car.setYear("2025");
    car.setPrice(20000);
    car.setStatus("ACTIVE");
    car.setUser(seller);
    carRepository.save(car);

    CarBidding bid = new CarBidding();
    bid.setCar(car);
    bid.setUser(buyer);
    bid.setBidPrice(21000);
    bid.setStatus("ONGOING");
    bidRepository.save(bid);

    PaymentOrder payment = paymentService.acceptBidForPayment(bid.getIdBid());
    payment.setCheckoutSessionId("cs_test_duplicate");
    payment.setStatus("CHECKOUT_CREATED");
    paymentRepository.save(payment);

    StripeWebhookEvent event = new StripeWebhookEvent(
        "evt_test_duplicate",
        "checkout.session.completed",
        "cs_test_duplicate",
        "pi_test_duplicate",
        "paid");
    paymentService.processWebhook(event);
    Instant paidAt = payment.getPaidAt();
    paymentService.processWebhook(event);

    assertEquals(paidAt, payment.getPaidAt());
    assertEquals("SOLD", car.getStatus());
  }
}
