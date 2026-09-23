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
import lithan.autostrada.auctions.notification.EndingSoonOutbox;
import lithan.autostrada.auctions.notification.NotificationClient;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.identity.CurrentIdentity;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.repository.AuctionFollowRepository;

import lithan.autostrada.auctions.repository.CarRepository;

@Service
public class AuctionNotificationService {

  private final EndingSoonOutbox outbox;
  private final NotificationClient notifications;
  private final AuctionFollowRepository followRepository;
  private final CarRepository carRepository;
  private final CurrentIdentity currentIdentity;
  private final Clock clock;
  private final Duration endingSoonWindow;

  public AuctionNotificationService(
      EndingSoonOutbox outbox, NotificationClient notifications,
      AuctionFollowRepository followRepository,
      CarRepository carRepository,
      CurrentIdentity currentIdentity,
      Clock clock,
      @Value("${auction.notifications.ending-soon-window:PT24H}")
          Duration endingSoonWindow) {
    this.outbox = outbox; this.notifications = notifications;
    this.followRepository = followRepository;
    this.carRepository = carRepository;
    this.currentIdentity = currentIdentity;
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
        if (createEndingSoonNotification(follow.getUserId(), car, now)) {
          created++;
        }
      }
    }
    return created;
  }

  @Transactional
  public boolean createEndingSoonNotification(int user, Car car) {
    return createEndingSoonNotification(user, car, LocalDateTime.now(clock));
  }

  private boolean createEndingSoonNotification(
      int user, Car car, LocalDateTime now) {
    if (!car.isEndingWithin(endingSoonWindow, now)) return false;
    return outbox.append(user, car, now, endingSoonWindow);
  }

  public long unreadCount() { return notifications.unreadCount(); }
}
