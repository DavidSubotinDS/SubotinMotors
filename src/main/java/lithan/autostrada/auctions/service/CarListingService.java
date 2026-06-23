package lithan.autostrada.auctions.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import lithan.autostrada.auctions.dto.CarListingForm;
import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.ListingTestRide;

public interface CarListingService {

  Page<CarListing> browse(String keyword, Pageable pageable);

  CarListing publicListing(int listingId);

  List<CarListing> currentUserListings();

  CarListing ownedListing(int listingId);

  CarListing create(CarListingForm form, MultipartFile image);

  CarListing update(int listingId, CarListingForm form, MultipartFile image);

  void activate(int listingId);

  void deactivate(int listingId);

  void scheduleTestRide(int listingId, LocalDateTime scheduledAt);

  List<ListingTestRide> currentUserTestRides();

  List<ListingTestRide> testRideRequestsForCurrentSeller();

  void rescheduleCurrentUserTestRide(int testRideId, LocalDateTime scheduledAt);

  void cancelCurrentUserTestRide(int testRideId);

  void acceptTestRideForOwnedListing(int testRideId);

  void rejectTestRideForOwnedListing(int testRideId);

  void cancelTestRideForOwnedListing(int testRideId);
}
