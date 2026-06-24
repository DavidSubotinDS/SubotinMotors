package lithan.autostrada.auctions;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarBidding;
import lithan.autostrada.auctions.entity.TestDrive;
import lithan.autostrada.auctions.entity.TestDriveStatus;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.repository.CarBiddingRepository;
import lithan.autostrada.auctions.repository.CarRepository;
import lithan.autostrada.auctions.repository.TestDriveRepository;
import lithan.autostrada.auctions.repository.UserRepository;
import lithan.autostrada.auctions.service.UserCarService;

@SpringBootTest
@Transactional
class UserCarSecurityIntegrationTests {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private CarRepository carRepository;

  @Autowired
  private CarBiddingRepository bidRepository;

  @Autowired
  private TestDriveRepository testDriveRepository;

  @Autowired
  private UserCarService userCarService;

  @Test
  @WithMockUser(username = "user123", roles = "USER")
  void newlyPostedCarsRequireAdministratorApproval() throws Exception {
    MockMultipartFile image = new MockMultipartFile(
        "imageFile",
        "demo.png",
        "image/png",
        Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="));
    Car car = new Car();
    car.setMake("Pending");
    car.setModel("Approval");
    car.setYear("2025");
    car.setPrice(15000);

    userCarService.postCar(image, car);

    assertEquals("PENDING", car.getStatus());
    assertThrows(IllegalStateException.class,
        () -> userCarService.changeOwnedCarStatus(car.getIdCar(), "ACTIVE"));

    userCarService.changeCarStatusByAdmin(car.getIdCar(), "ACTIVE");
    assertEquals("ACTIVE", carRepository.findById(car.getIdCar()).orElseThrow().getStatus());
  }

  @Test
  @WithMockUser(username = "user123", roles = "USER")
  void userCannotEditAnotherUsersCar() {
    UserAccount admin = userRepository.findByUsername("admin123").orElseThrow();
    Car car = new Car();
    car.setMake("Security");
    car.setModel("Check");
    car.setYear("2024");
    car.setPrice(10000);
    car.setStatus("ACTIVE");
    car.setUser(admin);
    carRepository.saveAndFlush(car);

    assertThrows(AccessDeniedException.class, () -> userCarService.getOwnedCarById(car.getIdCar()));
  }

  @Test
  @WithMockUser(username = "user123", roles = "USER")
  void userCanListAndCancelOwnOngoingBid() {
    Car car = saveActiveCarOwnedBy("admin123", "Bid", "Management");

    userCarService.placeBid(car.getIdCar(), 12000);
    CarBidding bid = userCarService.listCurrentUserBids().get(0);
    userCarService.cancelCurrentUserBid(bid.getIdBid());

    assertEquals("CANCELLED", bidRepository.findById(bid.getIdBid()).orElseThrow().getStatus());
  }

  @Test
  @WithMockUser(username = "user123", roles = "USER")
  void userCanRescheduleAndCancelOwnTestDrive() {
    Car car = saveActiveCarOwnedBy("admin123", "Test", "Drive");
    LocalDate originalDate = LocalDate.now().plusDays(5);
    LocalDate newDate = originalDate.plusDays(1);

    userCarService.saveTestDrive(originalDate, car.getIdCar());
    TestDrive testDrive = userCarService.listCurrentUserTestDrives().get(0);
    assertEquals(TestDriveStatus.PENDING, testDrive.getStatus());
    testDrive.setStatus(TestDriveStatus.ACCEPTED);
    testDriveRepository.saveAndFlush(testDrive);
    userCarService.rescheduleCurrentUserTestDrive(testDrive.getIdTestDrive(), newDate);

    TestDrive rescheduled = testDriveRepository.findById(testDrive.getIdTestDrive()).orElseThrow();
    assertEquals(newDate, rescheduled.getDate());
    assertEquals(TestDriveStatus.PENDING, rescheduled.getStatus());

    userCarService.cancelCurrentUserTestDrive(testDrive.getIdTestDrive());
    testDriveRepository.flush();

    TestDrive cancelled = testDriveRepository.findById(testDrive.getIdTestDrive()).orElseThrow();
    assertEquals(TestDriveStatus.CANCELLED, cancelled.getStatus());
    assertTrue(testDriveRepository.existsById(testDrive.getIdTestDrive()));
    assertThrows(IllegalStateException.class,
        () -> userCarService.rescheduleCurrentUserTestDrive(testDrive.getIdTestDrive(), newDate.plusDays(1)));
  }

  @Test
  @WithMockUser(username = "user123", roles = "USER")
  void testDriveMustBeAfterToday() {
    Car car = saveActiveCarOwnedBy("admin123", "Future", "Only");

    assertThrows(
        IllegalArgumentException.class,
        () -> userCarService.saveTestDrive(LocalDate.now(), car.getIdCar()));
  }

  @Test
  @WithMockUser(username = "admin123", roles = "USER")
  void ownerCanAcceptPendingTestDriveRequest() {
    UserAccount requester = userRepository.findByUsername("user123").orElseThrow();
    Car car = saveActiveCarOwnedBy("admin123", "Owner", "Approval");
    TestDrive testDrive = saveTestDrive(requester, car, LocalDate.now().plusDays(3));

    userCarService.acceptTestDriveForOwnedCar(testDrive.getIdTestDrive());

    assertEquals(TestDriveStatus.ACCEPTED,
        testDriveRepository.findById(testDrive.getIdTestDrive()).orElseThrow().getStatus());
    assertThrows(IllegalStateException.class,
        () -> userCarService.rejectTestDriveForOwnedCar(testDrive.getIdTestDrive()));

    userCarService.cancelTestDriveForOwnedCar(testDrive.getIdTestDrive());
    assertEquals(TestDriveStatus.CANCELLED,
        testDriveRepository.findById(testDrive.getIdTestDrive()).orElseThrow().getStatus());
  }

  @Test
  @WithMockUser(username = "admin123", roles = "USER")
  void ownerCanRejectPendingTestDriveRequest() {
    UserAccount requester = userRepository.findByUsername("user123").orElseThrow();
    Car car = saveActiveCarOwnedBy("admin123", "Owner", "Rejection");
    TestDrive testDrive = saveTestDrive(requester, car, LocalDate.now().plusDays(4));

    userCarService.rejectTestDriveForOwnedCar(testDrive.getIdTestDrive());

    assertEquals(TestDriveStatus.REJECTED,
        testDriveRepository.findById(testDrive.getIdTestDrive()).orElseThrow().getStatus());
  }

  @Test
  @WithMockUser(username = "user123", roles = "USER")
  void userCannotManageAnotherUsersBidOrTestDrive() {
    UserAccount admin = userRepository.findByUsername("admin123").orElseThrow();
    Car car = saveActiveCarOwnedBy("user123", "Ownership", "Boundary");

    CarBidding bid = new CarBidding();
    bid.setCar(car);
    bid.setUser(admin);
    bid.setBidPrice(12000);
    bid.setStatus("ONGOING");
    bidRepository.save(bid);

    TestDrive testDrive = new TestDrive();
    testDrive.setCar(car);
    testDrive.setUser(admin);
    testDrive.setDate(LocalDate.now().plusDays(3));
    testDrive.setStatus(TestDriveStatus.PENDING);
    testDriveRepository.save(testDrive);

    assertThrows(AccessDeniedException.class,
        () -> userCarService.cancelCurrentUserBid(bid.getIdBid()));
    assertThrows(AccessDeniedException.class,
        () -> userCarService.cancelCurrentUserTestDrive(testDrive.getIdTestDrive()));
  }

  @Test
  @WithMockUser(username = "user123", roles = "USER")
  void userCannotDecideRequestForAnotherOwnersCar() {
    UserAccount requester = userRepository.findByUsername("user123").orElseThrow();
    Car car = saveActiveCarOwnedBy("admin123", "Other", "Owner");
    TestDrive testDrive = saveTestDrive(requester, car, LocalDate.now().plusDays(7));

    assertThrows(AccessDeniedException.class,
        () -> userCarService.acceptTestDriveForOwnedCar(testDrive.getIdTestDrive()));
    assertThrows(AccessDeniedException.class,
        () -> userCarService.rejectTestDriveForOwnedCar(testDrive.getIdTestDrive()));
    assertThrows(AccessDeniedException.class,
        () -> userCarService.cancelTestDriveForOwnedCar(testDrive.getIdTestDrive()));
  }

  private TestDrive saveTestDrive(UserAccount requester, Car car, LocalDate date) {
    TestDrive testDrive = new TestDrive();
    testDrive.setCar(car);
    testDrive.setUser(requester);
    testDrive.setDate(date);
    testDrive.setStatus(TestDriveStatus.PENDING);
    return testDriveRepository.saveAndFlush(testDrive);
  }

  private Car saveActiveCarOwnedBy(String username, String make, String model) {
    UserAccount owner = userRepository.findByUsername(username).orElseThrow();
    Car car = new Car();
    car.setMake(make);
    car.setModel(model);
    car.setYear("2025");
    car.setPrice(10000);
    car.setStatus("ACTIVE");
    car.setUser(owner);
    return carRepository.saveAndFlush(car);
  }
}
