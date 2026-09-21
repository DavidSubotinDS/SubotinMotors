package lithan.autostrada.auctions.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.AuctionFollow;
import lithan.autostrada.auctions.entity.Car;

public interface AuctionFollowRepository extends JpaRepository<AuctionFollow, Integer> {

  boolean existsByUserIdAndCar(int userId, Car car);

  Optional<AuctionFollow> findByUserIdAndCar(int userId, Car car);

  List<AuctionFollow> findByUserIdOrderByCarAuctionEndTimeAsc(int userId);

  List<AuctionFollow> findByCar(Car car);
}
