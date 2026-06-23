package lithan.autostrada.auctions.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import lithan.autostrada.auctions.entity.CarBidding;
import lithan.autostrada.auctions.entity.UserAccount;

public interface CarBiddingRepository extends JpaRepository<CarBidding, Integer> {
  @Query("SELECT MAX(b.bidPrice) FROM CarBidding b WHERE b.car.idCar = :id AND b.status = 'ONGOING'")
  Integer highestBid(@Param("id") int id);

  List<CarBidding> findByStatusNot(String status);

  Page<CarBidding> findByStatusNot(String status, Pageable pageable);

  List<CarBidding> findByCarIdCar(int carId);

  List<CarBidding> findByUserOrderByIdBidDesc(UserAccount user);
}
