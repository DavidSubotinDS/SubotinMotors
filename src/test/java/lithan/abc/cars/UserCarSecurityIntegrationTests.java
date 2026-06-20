package lithan.abc.cars;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.CarBidding;
import lithan.abc.cars.entity.TestDrive;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.repository.CarBiddingRepository;
import lithan.abc.cars.repository.CarRepository;
import lithan.abc.cars.repository.TestDriveRepository;
import lithan.abc.cars.repository.UserRepository;
import lithan.abc.cars.service.UserCarService;

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
    userCarService.rescheduleCurrentUserTestDrive(testDrive.getIdTestDrive(), newDate);

    assertEquals(newDate,
        testDriveRepository.findById(testDrive.getIdTestDrive()).orElseThrow().getDate());

    userCarService.cancelCurrentUserTestDrive(testDrive.getIdTestDrive());
    testDriveRepository.flush();

    assertFalse(testDriveRepository.existsById(testDrive.getIdTestDrive()));
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
    testDriveRepository.save(testDrive);

    assertThrows(AccessDeniedException.class,
        () -> userCarService.cancelCurrentUserBid(bid.getIdBid()));
    assertThrows(AccessDeniedException.class,
        () -> userCarService.cancelCurrentUserTestDrive(testDrive.getIdTestDrive()));
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
