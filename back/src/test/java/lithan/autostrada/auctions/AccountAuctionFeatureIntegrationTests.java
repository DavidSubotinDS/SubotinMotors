package lithan.autostrada.auctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import fixtures.notification.entity.AuctionNotification;
import lithan.autostrada.auctions.entity.Car;
import fixtures.identity.entity.PasswordResetToken;
import fixtures.identity.entity.UserAccount;
import lithan.autostrada.auctions.repository.AuctionFollowRepository;
import fixtures.notification.repository.AuctionNotificationRepository;
import lithan.autostrada.auctions.repository.CarRepository;
import fixtures.identity.repository.PasswordResetTokenRepository;
import fixtures.identity.repository.UserRepository;
import lithan.autostrada.auctions.service.AuctionFollowService;
import lithan.autostrada.auctions.service.AuctionNotificationService;
import lithan.autostrada.auctions.service.UserCarService;

@org.springframework.context.annotation.Import(BusinessIdentityFixtures.class)
@SpringBootTest
@Transactional
class AccountAuctionFeatureIntegrationTests {



  @Autowired
  private UserRepository userRepository;


  @Autowired
  private CarRepository carRepository;

  @Autowired
  private UserCarService userCarService;

  @Autowired
  private AuctionFollowService followService;

  @Autowired
  private AuctionFollowRepository followRepository;

  @Autowired
  private AuctionNotificationService notificationService;

  @Autowired
  private AuctionNotificationRepository notificationRepository;
  @Autowired private org.springframework.jdbc.core.JdbcTemplate outboxSql;



  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void bidsAreRejectedAfterAuctionEnd() {
    Car car = saveCar("admin123", "Ended", "Auction", LocalDateTime.now().minusMinutes(1));

    IllegalStateException exception = assertThrows(
        IllegalStateException.class,
        () -> userCarService.placeBid(car.getIdCar(), 12_000));

    assertEquals("This auction has ended", exception.getMessage());
  }

  @Test
  void auctionStatusReflectsActiveEndingSoonEndedAndSoldStates() {
    LocalDateTime now = LocalDateTime.of(2026, 6, 20, 12, 0);
    Car car = new Car();
    car.setStatus("ACTIVE");
    car.setAuctionEndTime(now.plusDays(3));
    assertEquals("ACTIVE", car.auctionStatusAt(now));

    car.setAuctionEndTime(now.plusHours(2));
    assertEquals("ENDING_SOON", car.auctionStatusAt(now));

    car.setAuctionEndTime(now.minusSeconds(1));
    assertEquals("ENDED", car.auctionStatusAt(now));

    car.setStatus("SOLD");
    assertEquals("SOLD", car.auctionStatusAt(now));
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void followingIsUniqueAndCanBeRemoved() {
    Car car = saveCar("admin123", "Watch", "Target", LocalDateTime.now().plusDays(2));
    UserAccount user = userRepository.findByUsername("user123").orElseThrow();

    assertTrue(followService.follow(car.getIdCar()));
    assertFalse(followService.follow(car.getIdCar()));
    assertEquals(1, followRepository.findByUserIdOrderByCarAuctionEndTimeAsc(user.getIdUser()).stream()
        .filter(follow -> follow.getCar().getIdCar() == car.getIdCar())
        .count());

    assertTrue(followService.unfollow(car.getIdCar()));
    assertFalse(followService.unfollow(car.getIdCar()));
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void nearingEndNotificationIsCreatedOnceForFollowers() {
    Car car = saveCar("admin123", "Notify", "Target", LocalDateTime.now().plusDays(2));
    UserAccount user = userRepository.findByUsername("user123").orElseThrow();
    followService.follow(car.getIdCar());
    assertFalse(notificationRepository.existsByUserIdAndCarAndNotificationType(user.getIdUser(), car, AuctionNotification.ENDING_SOON));

    car.setAuctionEndTime(LocalDateTime.now().plusHours(2));
    carRepository.saveAndFlush(car);
    assertTrue(notificationService.createEndingSoonNotifications() >= 1);
    assertEquals(0, notificationService.createEndingSoonNotifications());
    assertFalse(notificationRepository.existsByUserIdAndCarAndNotificationType(user.getIdUser(), car, AuctionNotification.ENDING_SOON));
    assertEquals(1L,outboxSql.queryForObject("SELECT COUNT(*) FROM tb_notification_outbox WHERE dedupe_key=?",Long.class,
        user.getIdUser()+":"+car.getIdCar()+":ENDING_SOON"));
  }

  private Car saveCar(
      String ownerUsername,
      String make,
      String model,
      LocalDateTime auctionEndTime) {
    Car car = new Car();
    car.setMake(make);
    car.setModel(model);
    car.setYear("2025");
    car.setPrice(10_000);
    car.setStatus("ACTIVE");
    car.setAuctionEndTime(auctionEndTime);
    car.setUserId(userRepository.findByUsername(ownerUsername).orElseThrow().getIdUser());
    return carRepository.saveAndFlush(car);
  }
}
