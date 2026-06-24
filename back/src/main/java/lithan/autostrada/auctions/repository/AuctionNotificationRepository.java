package lithan.autostrada.auctions.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.AuctionNotification;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.UserAccount;

public interface AuctionNotificationRepository
    extends JpaRepository<AuctionNotification, Integer> {

  boolean existsByUserAndCarAndNotificationType(
      UserAccount user, Car car, String notificationType);

  List<AuctionNotification> findByUserOrderByCreatedAtDesc(UserAccount user);

  long countByUserAndReadAtIsNull(UserAccount user);

  List<AuctionNotification> findByUserAndReadAtIsNull(UserAccount user);
}
