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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.AuctionNotification;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.PasswordResetToken;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.repository.AuctionFollowRepository;
import lithan.autostrada.auctions.repository.AuctionNotificationRepository;
import lithan.autostrada.auctions.repository.CarRepository;
import lithan.autostrada.auctions.repository.PasswordResetTokenRepository;
import lithan.autostrada.auctions.repository.UserRepository;
import lithan.autostrada.auctions.service.AuctionFollowService;
import lithan.autostrada.auctions.service.AuctionNotificationService;
import lithan.autostrada.auctions.service.PasswordResetService;
import lithan.autostrada.auctions.service.UserCarService;

@SpringBootTest
@Transactional
class AccountAuctionFeatureIntegrationTests {

  @Autowired
  private PasswordResetService passwordResetService;

  @Autowired
  private PasswordResetTokenRepository tokenRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

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

  @Test
  void passwordResetTokenIsHashedConsumedAndChangesPassword() {
    UserAccount user = userRepository.findByUsername("user123").orElseThrow();
    String oldPassword = user.getPassword();

    String rawToken = passwordResetService.requestReset("user123").orElseThrow();
    PasswordResetToken stored = tokenRepository.findAll().stream()
        .filter(token -> token.getUser().getIdUser() == user.getIdUser())
        .findFirst()
        .orElseThrow();

    assertNotEquals(rawToken, stored.getTokenHash());
    assertTrue(passwordResetService.isValid(rawToken));
    assertTrue(passwordResetService.resetPassword(rawToken, "newSecret123"));
    assertFalse(passwordResetService.isValid(rawToken));
    assertFalse(passwordResetService.resetPassword(rawToken, "anotherSecret123"));
    assertTrue(passwordEncoder.matches(
        "newSecret123",
        userRepository.findById(user.getIdUser()).orElseThrow().getPassword()));
    assertFalse(passwordEncoder.matches(
        "newSecret123", oldPassword));
  }

  @Test
  void expiredAndInvalidResetTokensAreRejected() {
    UserAccount user = userRepository.findByUsername("user123").orElseThrow();
    String rawToken = passwordResetService.requestReset(user.getEmail()).orElseThrow();
    PasswordResetToken stored = tokenRepository.findAll().stream()
        .filter(token -> token.getUser().getIdUser() == user.getIdUser())
        .findFirst()
        .orElseThrow();
    stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    tokenRepository.saveAndFlush(stored);

    assertFalse(passwordResetService.isValid(rawToken));
    assertFalse(passwordResetService.resetPassword(rawToken, "newSecret123"));
    assertFalse(passwordResetService.isValid("not-a-real-token"));
  }

  @Test
  @WithMockUser(username = "user123", roles = "USER")
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
  @WithMockUser(username = "user123", roles = "USER")
  void followingIsUniqueAndCanBeRemoved() {
    Car car = saveCar("admin123", "Watch", "Target", LocalDateTime.now().plusDays(2));
    UserAccount user = userRepository.findByUsername("user123").orElseThrow();

    assertTrue(followService.follow(car.getIdCar()));
    assertFalse(followService.follow(car.getIdCar()));
    assertEquals(1, followRepository.findByUserOrderByCarAuctionEndTimeAsc(user).stream()
        .filter(follow -> follow.getCar().getIdCar() == car.getIdCar())
        .count());

    assertTrue(followService.unfollow(car.getIdCar()));
    assertFalse(followService.unfollow(car.getIdCar()));
  }

  @Test
  @WithMockUser(username = "user123", roles = "USER")
  void nearingEndNotificationIsCreatedOnceForFollowers() {
    Car car = saveCar("admin123", "Notify", "Target", LocalDateTime.now().plusDays(2));
    UserAccount user = userRepository.findByUsername("user123").orElseThrow();
    followService.follow(car.getIdCar());
    assertFalse(notificationRepository.existsByUserAndCarAndNotificationType(
        user, car, AuctionNotification.ENDING_SOON));

    car.setAuctionEndTime(LocalDateTime.now().plusHours(2));
    carRepository.saveAndFlush(car);
    assertTrue(notificationService.createEndingSoonNotifications() >= 1);
    assertEquals(0, notificationService.createEndingSoonNotifications());
    assertTrue(notificationRepository.existsByUserAndCarAndNotificationType(
        user, car, AuctionNotification.ENDING_SOON));
    assertEquals(1, notificationService.unreadCount());

    AuctionNotification notification =
        notificationRepository.findByUserOrderByCreatedAtDesc(user).get(0);
    notificationService.markRead(notification.getIdNotification());
    assertEquals(0, notificationService.unreadCount());
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
    car.setUser(userRepository.findByUsername(ownerUsername).orElseThrow());
    return carRepository.saveAndFlush(car);
  }
}
