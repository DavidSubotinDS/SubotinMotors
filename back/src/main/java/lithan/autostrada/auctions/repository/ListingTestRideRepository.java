package lithan.autostrada.auctions.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.ListingTestRide;
import lithan.autostrada.auctions.entity.UserAccount;

public interface ListingTestRideRepository extends JpaRepository<ListingTestRide, Integer> {

  List<ListingTestRide> findByUserOrderByScheduledAtAsc(UserAccount user);

  List<ListingTestRide> findByListingSellerOrderByScheduledAtAsc(UserAccount seller);

  boolean existsByUserAndListingAndScheduledAt(
      UserAccount user, CarListing listing, LocalDateTime scheduledAt);

  boolean existsByUserAndListingAndScheduledAtAndIdTestRideNot(
      UserAccount user, CarListing listing, LocalDateTime scheduledAt, int idTestRide);
}
