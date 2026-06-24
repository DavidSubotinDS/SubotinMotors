package lithan.autostrada.auctions.service;

import java.math.BigDecimal;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lithan.autostrada.auctions.dto.CarListingForm;
import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.CarListingPicture;
import lithan.autostrada.auctions.entity.CarListingStatus;
import lithan.autostrada.auctions.entity.ListingTestRide;
import lithan.autostrada.auctions.entity.TestDriveStatus;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.repository.CarListingRepository;
import lithan.autostrada.auctions.repository.ListingTestRideRepository;
import lithan.autostrada.auctions.validation.ImageUploadValidator;
import lithan.autostrada.auctions.validation.ImageUploadValidator.ValidatedImage;

@Service
public class CarListingServiceImpl implements CarListingService {

  private final CarListingRepository listingRepository;
  private final ListingTestRideRepository testRideRepository;
  private final UserService userService;

  public CarListingServiceImpl(
      CarListingRepository listingRepository,
      ListingTestRideRepository testRideRepository,
      UserService userService) {
    this.listingRepository = listingRepository;
    this.testRideRepository = testRideRepository;
    this.userService = userService;
  }

  @Override
  public Page<CarListing> browse(String keyword, Pageable pageable) {
    String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
    return listingRepository.browse(
        Set.of(CarListingStatus.ACTIVE, CarListingStatus.RESERVED),
        normalized,
        pageable);
  }

  @Override
  public CarListing publicListing(int listingId) {
    CarListing listing = listingRepository.findById(listingId)
        .orElseThrow(ResourceNotFoundException::new);
    if (listing.getStatus() == CarListingStatus.INACTIVE) {
      throw new ResourceNotFoundException();
    }
    return listing;
  }

  @Override
  public List<CarListing> currentUserListings() {
    return listingRepository.findBySellerOrderByCreatedAtDesc(userService.getUserLogin());
  }

  @Override
  public CarListing ownedListing(int listingId) {
    CarListing listing = listingRepository.findById(listingId)
        .orElseThrow(ResourceNotFoundException::new);
    if (listing.getSeller().getIdUser() != userService.getUserLogin().getIdUser()) {
      throw new AccessDeniedException("You do not own this listing");
    }
    return listing;
  }

  @Override
  @Transactional
  public CarListing create(CarListingForm form, MultipartFile image) {
    if (image == null || image.isEmpty()) {
      throw new IllegalArgumentException("Car picture is required");
    }
    ValidatedImage validatedImage = validateImage(image);
    Instant now = Instant.now();
    CarListing listing = new CarListing();
    apply(form, listing);
    listing.setSeller(userService.getUserLogin());
    listing.setStatus(CarListingStatus.ACTIVE);
    listing.setCreatedAt(now);
    listing.setUpdatedAt(now);
    setPicture(listing, validatedImage);
    return listingRepository.save(listing);
  }

  @Override
  @Transactional
  public CarListing update(int listingId, CarListingForm form, MultipartFile image) {
    CarListing listing = ownedListing(listingId);
    if (listing.getStatus() == CarListingStatus.SOLD
        || listing.getStatus() == CarListingStatus.RESERVED) {
      throw new IllegalStateException("Reserved or sold listings cannot be edited");
    }
    apply(form, listing);
    if (image != null && !image.isEmpty()) {
      setPicture(listing, validateImage(image));
    }
    listing.setUpdatedAt(Instant.now());
    return listingRepository.save(listing);
  }

  @Override
  @Transactional
  public void activate(int listingId) {
    changeStatus(listingId, CarListingStatus.ACTIVE);
  }

  @Override
  @Transactional
  public void deactivate(int listingId) {
    changeStatus(listingId, CarListingStatus.INACTIVE);
  }

  private void changeStatus(int listingId, CarListingStatus target) {
    CarListing listing = ownedListing(listingId);
    if (listing.getStatus() == CarListingStatus.RESERVED
        || listing.getStatus() == CarListingStatus.SOLD) {
      throw new IllegalStateException("Reserved or sold listings cannot be reactivated or deactivated");
    }
    listing.setStatus(target);
    listing.setUpdatedAt(Instant.now());
    listingRepository.save(listing);
  }

  @Override
  @Transactional
  public void scheduleTestRide(int listingId, LocalDateTime scheduledAt) {
    validateFuture(scheduledAt);
    CarListing listing = publicListing(listingId);
    UserAccount requester = userService.getUserLogin();
    if (!listing.isActive()) {
      throw new IllegalStateException("Test rides are only available for active listings");
    }
    if (listing.getSeller().getIdUser() == requester.getIdUser()) {
      throw new IllegalStateException("You cannot schedule a test ride for your own listing");
    }
    if (testRideRepository.existsByUserAndListingAndScheduledAt(
        requester, listing, scheduledAt)) {
      throw new IllegalStateException("You already requested this time for the listing");
    }

    Instant now = Instant.now();
    ListingTestRide testRide = new ListingTestRide();
    testRide.setListing(listing);
    testRide.setUser(requester);
    testRide.setScheduledAt(scheduledAt);
    testRide.setStatus(TestDriveStatus.PENDING);
    testRide.setCreatedAt(now);
    testRide.setUpdatedAt(now);
    testRideRepository.save(testRide);
  }

  @Override
  public List<ListingTestRide> currentUserTestRides() {
    return testRideRepository.findByUserOrderByScheduledAtAsc(userService.getUserLogin());
  }

  @Override
  public List<ListingTestRide> testRideRequestsForCurrentSeller() {
    return testRideRepository.findByListingSellerOrderByScheduledAtAsc(userService.getUserLogin());
  }

  @Override
  @Transactional
  public void rescheduleCurrentUserTestRide(int testRideId, LocalDateTime scheduledAt) {
    validateFuture(scheduledAt);
    ListingTestRide testRide = currentUserTestRide(testRideId);
    if (!testRide.isReschedulable()) {
      throw new IllegalStateException("Only pending or accepted test rides can be rescheduled");
    }
    if (!testRide.getListing().isActive()) {
      throw new IllegalStateException("Only active listings can be rescheduled");
    }
    if (testRideRepository.existsByUserAndListingAndScheduledAtAndIdTestRideNot(
        testRide.getUser(), testRide.getListing(), scheduledAt, testRideId)) {
      throw new IllegalStateException("You already requested this time for the listing");
    }
    testRide.setScheduledAt(scheduledAt);
    testRide.setStatus(TestDriveStatus.PENDING);
    testRide.setUpdatedAt(Instant.now());
    testRideRepository.save(testRide);
  }

  @Override
  @Transactional
  public void cancelCurrentUserTestRide(int testRideId) {
    ListingTestRide testRide = currentUserTestRide(testRideId);
    if (!testRide.isCancellable()) {
      throw new IllegalStateException("Only pending or accepted test rides can be cancelled");
    }
    setTestRideStatus(testRide, TestDriveStatus.CANCELLED);
  }

  @Override
  @Transactional
  public void acceptTestRideForOwnedListing(int testRideId) {
    decideOwnedTestRide(testRideId, TestDriveStatus.ACCEPTED);
  }

  @Override
  @Transactional
  public void rejectTestRideForOwnedListing(int testRideId) {
    decideOwnedTestRide(testRideId, TestDriveStatus.REJECTED);
  }

  @Override
  @Transactional
  public void cancelTestRideForOwnedListing(int testRideId) {
    ListingTestRide testRide = ownedListingTestRide(testRideId);
    if (!testRide.isAccepted()) {
      throw new IllegalStateException("Only accepted test rides can be cancelled by the seller");
    }
    setTestRideStatus(testRide, TestDriveStatus.CANCELLED);
  }

  private void decideOwnedTestRide(int testRideId, TestDriveStatus status) {
    ListingTestRide testRide = ownedListingTestRide(testRideId);
    if (!testRide.isPending()) {
      throw new IllegalStateException("Only pending test-ride requests can be accepted or rejected");
    }
    setTestRideStatus(testRide, status);
  }

  private ListingTestRide currentUserTestRide(int testRideId) {
    ListingTestRide testRide = testRideRepository.findById(testRideId)
        .orElseThrow(ResourceNotFoundException::new);
    if (testRide.getUser().getIdUser() != userService.getUserLogin().getIdUser()) {
      throw new AccessDeniedException("This test ride belongs to another user");
    }
    return testRide;
  }

  private ListingTestRide ownedListingTestRide(int testRideId) {
    ListingTestRide testRide = testRideRepository.findById(testRideId)
        .orElseThrow(ResourceNotFoundException::new);
    if (testRide.getListing().getSeller().getIdUser()
        != userService.getUserLogin().getIdUser()) {
      throw new AccessDeniedException("This request belongs to another seller");
    }
    return testRide;
  }

  private void setTestRideStatus(ListingTestRide testRide, TestDriveStatus status) {
    testRide.setStatus(status);
    testRide.setUpdatedAt(Instant.now());
    testRideRepository.save(testRide);
  }

  private void apply(CarListingForm form, CarListing listing) {
    listing.setTitle(form.getTitle().trim());
    listing.setMake(form.getMake().trim());
    listing.setModel(form.getModel().trim());
    listing.setYear(form.getYear().trim());
    listing.setMileage(form.getMileage());
    listing.setFuelType(form.getFuelType().trim());
    listing.setTransmission(form.getTransmission().trim());
    listing.setPriceMinor(toMinor(form.getPrice()));
    listing.setDepositAmountMinor(toMinor(form.getDepositAmount()));
    listing.setDescription(form.getDescription().trim());
  }

  private long toMinor(BigDecimal amount) {
    return amount.movePointRight(2).longValueExact();
  }

  private void setPicture(CarListing listing, ValidatedImage image) {
    CarListingPicture picture = listing.getPicture();
    if (picture == null) {
      picture = new CarListingPicture();
      picture.setListing(listing);
      listing.setPicture(picture);
    }
    picture.setFileName(image.fileName());
    picture.setFileType(image.contentType());
    picture.setImage(Base64.getEncoder().encodeToString(image.bytes()));
  }

  private void validateFuture(LocalDateTime scheduledAt) {
    if (scheduledAt == null || !scheduledAt.isAfter(LocalDateTime.now())) {
      throw new IllegalArgumentException("Test ride date and time must be in the future");
    }
  }

  private ValidatedImage validateImage(MultipartFile image) {
    try {
      return ImageUploadValidator.validate(image);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to read image", exception);
    }
  }
}
