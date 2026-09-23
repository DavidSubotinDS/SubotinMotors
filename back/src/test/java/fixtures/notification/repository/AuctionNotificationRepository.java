package fixtures.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import fixtures.notification.entity.AuctionNotification;
import lithan.autostrada.auctions.entity.Car;

public interface AuctionNotificationRepository
    extends JpaRepository<AuctionNotification, Integer> {

  boolean existsByUserIdAndCarAndNotificationType(
      int userId, Car car, String notificationType);

  List<AuctionNotification> findByUserIdOrderByCreatedAtDesc(int userId);

  long countByUserIdAndReadAtIsNull(int userId);

  List<AuctionNotification> findByUserIdAndReadAtIsNull(int userId);
}
