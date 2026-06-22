package lithan.abc.cars.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.AuctionFollow;
import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.UserAccount;

public interface AuctionFollowRepository extends JpaRepository<AuctionFollow, Integer> {

  boolean existsByUserAndCar(UserAccount user, Car car);

  Optional<AuctionFollow> findByUserAndCar(UserAccount user, Car car);

  List<AuctionFollow> findByUserOrderByCarAuctionEndTimeAsc(UserAccount user);

  List<AuctionFollow> findByCar(Car car);
}
