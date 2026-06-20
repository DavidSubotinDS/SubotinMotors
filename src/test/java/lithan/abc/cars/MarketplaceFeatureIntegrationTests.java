package lithan.abc.cars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarBidding;
import lithan.abc.cars.entity.PaymentOrder;
import lithan.abc.cars.entity.PaymentWebhookEvent;
import lithan.abc.cars.entity.Role;
import lithan.abc.cars.entity.TestDrive;
import lithan.abc.cars.entity.TestDriveStatus;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.repository.CarBiddingRepository;
import lithan.abc.cars.repository.CarRepository;
import lithan.abc.cars.repository.PaymentOrderRepository;
import lithan.abc.cars.repository.PaymentWebhookEventRepository;
import lithan.abc.cars.repository.TestDriveRepository;
import lithan.abc.cars.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MarketplaceFeatureIntegrationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CarRepository carRepository;

  @Autowired
  private CarBiddingRepository bidRepository;

  @Autowired
  private TestDriveRepository testDriveRepository;

  @Autowired
  private PaymentOrderRepository paymentRepository;

  @Autowired
  private PaymentWebhookEventRepository webhookEventRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void registrationCreatesAccountProfileAndUserRole() throws Exception {
    MvcResult accountResult = mockMvc.perform(post("/register/accountProcess")
            .with(csrf())
            .param("username", "newdriver")
            .param("password", "secret123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/register/profile"))
        .andReturn();

    MockHttpSession session = (MockHttpSession) accountResult.getRequest().getSession(false);
    assertNotNull(session);
    assertNotNull(session.getAttribute("registerAccount"));

    mockMvc.perform(post("/register/profileProcess")
            .session(session)
            .with(csrf())
            .param("firstName", "New")
            .param("lastName", "Driver")
            .param("phoneNumber", "+381601234567")
            .param("address", "Novi Sad")
            .param("about", "Test account"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/register/thank-you"));

    UserAccount registered = userRepository.findByUsername("newdriver").orElseThrow();
    assertTrue(passwordEncoder.matches("secret123", registered.getPassword()));
    assertEquals("New", registered.getProfile().getFirstName());
    assertEquals("Driver", registered.getProfile().getLastName());
    assertEquals(List.of("ROLE_USER"),
        registered.getRoles().stream().map(Role::getRole).toList());
  }

  @Test
  void duplicateUsernameIsRejectedCaseInsensitively() throws Exception {
    long accountCount = userRepository.count();

    MvcResult accountResult = mockMvc.perform(post("/register/accountProcess")
            .with(csrf())
            .param("username", "USER123")
            .param("password", "secret123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/register/profile"))
        .andReturn();

    MockHttpSession session = (MockHttpSession) accountResult.getRequest().getSession(false);
    assertNotNull(session);

    mockMvc.perform(post("/register/profileProcess")
            .session(session)
            .with(csrf())
            .param("firstName", "Duplicate")
            .param("lastName", "User")
            .param("phoneNumber", "0612345678")
            .param("address", "Belgrade"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/register/account?duplicate"));

    assertEquals(accountCount, userRepository.count());
    assertNull(session.getAttribute("registerAccount"));
  }

  @Test
  void catalogueSearchFiltersPaginatesAndSortsActualCars() throws Exception {
    UserAccount owner = userRepository.findByUsername("admin123").orElseThrow();
    saveCar(owner, "CoverageBatch", "Economy", 12_000, "ACTIVE");
    saveCar(owner, "CoverageBatch", "Executive", 18_000, "ACTIVE");
    saveCar(owner, "CoverageBatch", "Family", 15_000, "ACTIVE");
    saveCar(owner, "CoverageBatch", "Hidden", 16_000, "DEACTIVE");
    saveCar(owner, "CoverageBatch", "Luxury", 40_000, "ACTIVE");

    MvcResult firstResult = mockMvc.perform(get("/cars")
            .param("page", "0")
            .param("size", "2")
            .param("keyword", "coveragebatch")
            .param("low", "10000")
            .param("high", "20000")
            .param("sort", "price")
            .param("direction", "desc"))
        .andExpect(status().isOk())
        .andExpect(view().name("cars"))
        .andExpect(model().attribute("sort", "price"))
        .andExpect(model().attribute("direction", "desc"))
        .andReturn();

    Page<Car> firstPage = carPage(firstResult);
    assertEquals(3, firstPage.getTotalElements());
    assertEquals(2, firstPage.getTotalPages());
    assertEquals(List.of(18_000, 15_000),
        firstPage.getContent().stream().map(Car::getPrice).toList());

    MvcResult secondResult = mockMvc.perform(get("/cars")
            .param("page", "1")
            .param("size", "2")
            .param("keyword", "CoverageBatch")
            .param("low", "10000")
            .param("high", "20000")
            .param("sort", "price")
            .param("direction", "desc"))
        .andExpect(status().isOk())
        .andReturn();

    Page<Car> secondPage = carPage(secondResult);
    assertEquals(List.of(12_000),
        secondPage.getContent().stream().map(Car::getPrice).toList());
  }

  @Test
  void adminRoutesRejectAnonymousAndRegularUsers() throws Exception {
    mockMvc.perform(get("/admin/transactions"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));

    for (String path : List.of(
        "/admin/dashboard", "/admin/car-management", "/admin/transactions")) {
      mockMvc.perform(get(path).with(user("user123").roles("USER")))
          .andExpect(status().isForbidden());
    }

    int targetUserId = userRepository.findByUsername("user123").orElseThrow().getIdUser();
    mockMvc.perform(post("/admin/mark-admin/{idUser}", targetUserId)
            .with(user("user123").roles("USER"))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidRegistrationStaysOnAccountStepWithFieldErrors() throws Exception {
    MvcResult result = mockMvc.perform(post("/register/accountProcess")
            .with(csrf())
            .param("username", "ab")
            .param("password", "123"))
        .andExpect(status().isOk())
        .andExpect(view().name("register-account"))
        .andExpect(model().attributeHasFieldErrors("account", "username", "password"))
        .andReturn();

    assertNull(result.getRequest().getSession().getAttribute("registerAccount"));
  }

  @Test
  void invalidCarBidAndTestDriveFormsDoNotWriteData() throws Exception {
    UserAccount owner = userRepository.findByUsername("admin123").orElseThrow();
    Car car = saveCar(owner, "Validation", "Target", 10_000, "ACTIVE");
    long carCount = carRepository.count();
    long bidCount = bidRepository.count();
    long testDriveCount = testDriveRepository.count();

    MockMultipartFile emptyImage = new MockMultipartFile(
        "imageFile", "", "application/octet-stream", new byte[0]);
    mockMvc.perform(multipart("/user/postCarProcess")
            .file(emptyImage)
            .param("make", "")
            .param("model", "")
            .param("year", "1885")
            .param("price", "0")
            .with(user("user123").roles("USER"))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("user/post-car"))
        .andExpect(model().attributeHasFieldErrors("car", "make", "model", "year", "price"));

    mockMvc.perform(post("/postCarBidding")
            .param("carId", String.valueOf(car.getIdCar()))
            .param("bidPrice", "0")
            .with(user("user123").roles("USER"))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("user/car-bid"))
        .andExpect(model().attributeHasFieldErrors("carBidding", "bidPrice"));

    mockMvc.perform(post("/test-drive/testDriveProcess")
            .param("carId", String.valueOf(car.getIdCar()))
            .param("date", LocalDate.now().toString())
            .with(user("user123").roles("USER"))
            .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("user/test-drive"))
        .andExpect(model().attributeHasFieldErrors("testDrive", "date"));

    assertEquals(carCount, carRepository.count());
    assertEquals(bidCount, bidRepository.count());
    assertEquals(testDriveCount, testDriveRepository.count());
  }

  @Test
  void testDriveWorkflowCanBeBookedAcceptedRescheduledAndCancelled() throws Exception {
    UserAccount owner = userRepository.findByUsername("admin123").orElseThrow();
    UserAccount requester = userRepository.findByUsername("user123").orElseThrow();
    Car car = saveCar(owner, "Workflow", "Roadster", 22_000, "ACTIVE");
    LocalDate originalDate = LocalDate.now().plusDays(5);
    LocalDate rescheduledDate = originalDate.plusDays(2);

    mockMvc.perform(post("/test-drive/testDriveProcess")
            .param("carId", String.valueOf(car.getIdCar()))
            .param("date", originalDate.toString())
            .with(user("user123").roles("USER"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));

    TestDrive testDrive = testDriveRepository.findByUserOrderByDateAsc(requester).stream()
        .filter(booking -> booking.getCar().getIdCar() == car.getIdCar())
        .findFirst()
        .orElseThrow();
    assertEquals(TestDriveStatus.PENDING, testDrive.getStatus());

    mockMvc.perform(get("/user/test-drive")
            .with(user("user123").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(model().attribute("bookedTestDrives",
            org.hamcrest.Matchers.hasItem(
                org.hamcrest.Matchers.hasProperty("idTestDrive",
                    org.hamcrest.Matchers.is(testDrive.getIdTestDrive())))));

    mockMvc.perform(post("/user/test-drives/{idTestDrive}/accept", testDrive.getIdTestDrive())
            .with(user("admin123").roles("USER", "ADMIN"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/user/test-drive"))
        .andExpect(flash().attribute("appointmentMessage", "Test-drive request accepted."));
    assertEquals(TestDriveStatus.ACCEPTED,
        testDriveRepository.findById(testDrive.getIdTestDrive()).orElseThrow().getStatus());

    mockMvc.perform(post("/user/test-drives/{idTestDrive}/reschedule", testDrive.getIdTestDrive())
            .param("date", rescheduledDate.toString())
            .with(user("user123").roles("USER"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("appointmentMessage", "Test drive rescheduled."));

    TestDrive rescheduled = testDriveRepository.findById(testDrive.getIdTestDrive()).orElseThrow();
    assertEquals(rescheduledDate, rescheduled.getDate());
    assertEquals(TestDriveStatus.PENDING, rescheduled.getStatus());

    mockMvc.perform(post("/user/test-drives/{idTestDrive}/cancel", testDrive.getIdTestDrive())
            .with(user("user123").roles("USER"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attribute("appointmentMessage", "Test drive cancelled."));
    assertEquals(TestDriveStatus.CANCELLED,
        testDriveRepository.findById(testDrive.getIdTestDrive()).orElseThrow().getStatus());
  }

  @Test
  void adminTransactionPageDisplaysSortedPaymentsAndWebhookAuditData() throws Exception {
    UserAccount seller = userRepository.findByUsername("admin123").orElseThrow();
    UserAccount buyer = userRepository.findByUsername("user123").orElseThrow();

    PaymentOrder expensive = savePayment(
        seller, buyer, "Transaction", "Premium", 22_000, 2_200_000L,
        "PAID", "pi_expensive", Instant.parse("2026-06-19T10:15:30Z"));
    PaymentOrder affordable = savePayment(
        seller, buyer, "Transaction", "Budget", 11_000, 1_100_000L,
        "CHECKOUT_CREATED", null, null);

    PaymentWebhookEvent event = new PaymentWebhookEvent();
    event.setProviderEventId("evt_admin_display");
    event.setEventType("checkout.session.completed");
    event.setProcessedAt(Instant.parse("2026-06-19T10:16:00Z"));
    webhookEventRepository.saveAndFlush(event);

    MvcResult result = mockMvc.perform(get("/admin/transactions")
            .param("sort", "amountMinor")
            .param("direction", "asc")
            .with(user("admin123").roles("USER", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/transactions"))
        .andExpect(model().attribute("sort", "amountMinor"))
        .andExpect(model().attribute("direction", "asc"))
        .andReturn();

    List<PaymentOrder> transactions = transactionList(result).stream()
        .filter(payment -> "Transaction".equals(payment.getBid().getCar().getMake()))
        .toList();
    assertEquals(List.of(affordable.getIdPayment(), expensive.getIdPayment()),
        transactions.stream().map(PaymentOrder::getIdPayment).toList());
    assertEquals("user123", transactions.get(0).getBuyer().getUsername());
    assertEquals("admin123", transactions.get(0).getSeller().getUsername());
    assertEquals("Budget", transactions.get(0).getBid().getCar().getModel());
    assertEquals(1_100_000L, transactions.get(0).getAmountMinor());
    assertFalse(transactions.get(0).getStatus().isBlank());

    List<PaymentWebhookEvent> webhookEvents = webhookList(result);
    assertTrue(webhookEvents.stream()
        .anyMatch(webhook -> "evt_admin_display".equals(webhook.getProviderEventId())));
  }

  @SuppressWarnings("unchecked")
  private Page<Car> carPage(MvcResult result) {
    assertNotNull(result.getModelAndView());
    return (Page<Car>) result.getModelAndView().getModel().get("carPage");
  }

  @SuppressWarnings("unchecked")
  private List<PaymentOrder> transactionList(MvcResult result) {
    assertNotNull(result.getModelAndView());
    return (List<PaymentOrder>) result.getModelAndView().getModel().get("transactions");
  }

  @SuppressWarnings("unchecked")
  private List<PaymentWebhookEvent> webhookList(MvcResult result) {
    assertNotNull(result.getModelAndView());
    return (List<PaymentWebhookEvent>) result.getModelAndView().getModel().get("webhookEvents");
  }

  private Car saveCar(UserAccount owner, String make, String model, int price, String status) {
    Car car = new Car();
    car.setMake(make);
    car.setModel(model);
    car.setYear("2025");
    car.setPrice(price);
    car.setStatus(status);
    car.setUser(owner);
    return carRepository.saveAndFlush(car);
  }

  private PaymentOrder savePayment(
      UserAccount seller,
      UserAccount buyer,
      String make,
      String model,
      int bidPrice,
      long amountMinor,
      String status,
      String paymentIntentId,
      Instant paidAt) {
    Car car = saveCar(seller, make, model, bidPrice - 1_000, "SOLD");

    CarBidding bid = new CarBidding();
    bid.setCar(car);
    bid.setUser(buyer);
    bid.setBidPrice(bidPrice);
    bid.setStatus("PAID");
    bidRepository.saveAndFlush(bid);

    Instant createdAt = Instant.parse("2026-06-18T09:00:00Z");
    PaymentOrder payment = new PaymentOrder();
    payment.setBid(bid);
    payment.setBuyer(buyer);
    payment.setSeller(seller);
    payment.setAmountMinor(amountMinor);
    payment.setPlatformFeeMinor(amountMinor / 40);
    payment.setCurrency("eur");
    payment.setStatus(status);
    payment.setCheckoutSessionId("cs_" + model.toLowerCase());
    payment.setPaymentIntentId(paymentIntentId);
    payment.setCreatedAt(createdAt);
    payment.setUpdatedAt(paidAt == null ? createdAt : paidAt);
    payment.setPaidAt(paidAt);
    return paymentRepository.saveAndFlush(payment);
  }
}
