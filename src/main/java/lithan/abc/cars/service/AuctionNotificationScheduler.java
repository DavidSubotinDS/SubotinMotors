package lithan.abc.cars.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "auction.notifications.scheduling-enabled",
    havingValue = "true",
    matchIfMissing = true)
public class AuctionNotificationScheduler {

  private final AuctionNotificationService notificationService;

  public AuctionNotificationScheduler(AuctionNotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Scheduled(fixedDelayString = "${auction.notifications.scan-interval-ms:300000}")
  public void createEndingSoonNotifications() {
    notificationService.createEndingSoonNotifications();
  }
}
