package lithan.autostrada.auctions.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.ListingTestRide;

public interface ListingTestRideRepository extends JpaRepository<ListingTestRide, Integer> {

  List<ListingTestRide> findByUserIdOrderByScheduledAtAsc(int userId);

  List<ListingTestRide> findByListingSellerIdOrderByScheduledAtAsc(int sellerId);

  boolean existsByUserIdAndListingAndScheduledAt(
      int userId, CarListing listing, LocalDateTime scheduledAt);

  boolean existsByUserIdAndListingAndScheduledAtAndIdTestRideNot(
      int userId, CarListing listing, LocalDateTime scheduledAt, int idTestRide);
}
