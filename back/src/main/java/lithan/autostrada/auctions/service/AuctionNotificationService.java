package lithan.autostrada.auctions.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.AuctionFollow;
import lithan.autostrada.auctions.entity.AuctionNotification;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.repository.AuctionFollowRepository;
import lithan.autostrada.auctions.repository.AuctionNotificationRepository;
import lithan.autostrada.auctions.repository.CarRepository;

@Service
public class AuctionNotificationService {

  private final AuctionNotificationRepository notificationRepository;
  private final AuctionFollowRepository followRepository;
  private final CarRepository carRepository;
  private final UserService userService;
  private final Clock clock;
  private final Duration endingSoonWindow;

  public AuctionNotificationService(
      AuctionNotificationRepository notificationRepository,
      AuctionFollowRepository followRepository,
      CarRepository carRepository,
      UserService userService,
      Clock clock,
      @Value("${auction.notifications.ending-soon-window:PT24H}")
          Duration endingSoonWindow) {
    this.notificationRepository = notificationRepository;
    this.followRepository = followRepository;
    this.carRepository = carRepository;
    this.userService = userService;
    this.clock = clock;
    this.endingSoonWindow = endingSoonWindow;
  }

  @Transactional
  public int createEndingSoonNotifications() {
    LocalDateTime now = LocalDateTime.now(clock);
    List<Car> endingCars =
        carRepository.findByStatusAndAuctionEndTimeAfterAndAuctionEndTimeLessThanEqual(
            "ACTIVE", now, now.plus(endingSoonWindow));
    int created = 0;
    for (Car car : endingCars) {
      for (AuctionFollow follow : followRepository.findByCar(car)) {
        if (createEndingSoonNotification(follow.getUser(), car, now)) {
          created++;
        }
      }
    }
    return created;
  }

  @Transactional
  public boolean createEndingSoonNotification(UserAccount user, Car car) {
    return createEndingSoonNotification(user, car, LocalDateTime.now(clock));
  }

  private boolean createEndingSoonNotification(
      UserAccount user, Car car, LocalDateTime now) {
    if (!car.isEndingWithin(endingSoonWindow, now)
        || notificationRepository.existsByUserAndCarAndNotificationType(
            user, car, AuctionNotification.ENDING_SOON)) {
      return false;
    }

    AuctionNotification notification = new AuctionNotification();
    notification.setUser(user);
    notification.setCar(car);
    notification.setNotificationType(AuctionNotification.ENDING_SOON);
    notification.setMessage(
        car.getMake() + " " + car.getModel() + " is ending soon.");
    notification.setCreatedAt(now);
    notificationRepository.save(notification);
    return true;
  }

  public List<AuctionNotification> listCurrentUserNotifications() {
    return notificationRepository.findByUserOrderByCreatedAtDesc(
        userService.getUserLogin());
  }

  public long unreadCount() {
    return notificationRepository.countByUserAndReadAtIsNull(
        userService.getUserLogin());
  }

  @Transactional
  public void markRead(int notificationId) {
    AuctionNotification notification = notificationRepository.findById(notificationId)
        .orElseThrow(ResourceNotFoundException::new);
    UserAccount currentUser = userService.getUserLogin();
    if (notification.getUser().getIdUser() != currentUser.getIdUser()) {
      throw new AccessDeniedException("This notification belongs to another user");
    }
    if (!notification.isRead()) {
      notification.setReadAt(LocalDateTime.now(clock));
      notificationRepository.save(notification);
    }
  }

  @Transactional
  public void markAllRead() {
    UserAccount user = userService.getUserLogin();
    LocalDateTime now = LocalDateTime.now(clock);
    List<AuctionNotification> unread =
        notificationRepository.findByUserAndReadAtIsNull(user);
    unread.forEach(notification -> notification.setReadAt(now));
    notificationRepository.saveAll(unread);
  }
}
