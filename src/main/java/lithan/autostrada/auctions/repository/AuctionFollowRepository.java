package lithan.autostrada.auctions.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.AuctionFollow;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.UserAccount;

public interface AuctionFollowRepository extends JpaRepository<AuctionFollow, Integer> {

  boolean existsByUserAndCar(UserAccount user, Car car);

  Optional<AuctionFollow> findByUserAndCar(UserAccount user, Car car);

  List<AuctionFollow> findByUserOrderByCarAuctionEndTimeAsc(UserAccount user);

  List<AuctionFollow> findByCar(Car car);
}
