package lithan.abc.cars.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.AuctionNotification;
import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.UserAccount;

public interface AuctionNotificationRepository
    extends JpaRepository<AuctionNotification, Integer> {

  boolean existsByUserAndCarAndNotificationType(
      UserAccount user, Car car, String notificationType);

  List<AuctionNotification> findByUserOrderByCreatedAtDesc(UserAccount user);

  long countByUserAndReadAtIsNull(UserAccount user);

  List<AuctionNotification> findByUserAndReadAtIsNull(UserAccount user);
}
