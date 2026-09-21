package lithan.autostrada.auctions.service;

import java.math.BigDecimal;
import java.io.IOException;
import java.time.Instant;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lithan.autostrada.auctions.dto.CarListingForm;
import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.CarListingGalleryPicture;
import lithan.autostrada.auctions.entity.CarListingPicture;
import lithan.autostrada.auctions.entity.CarListingStatus;
import lithan.autostrada.auctions.entity.ListingTestRide;
import lithan.autostrada.auctions.entity.TestDriveStatus;
import lithan.autostrada.auctions.identity.CurrentIdentity;
import lithan.autostrada.auctions.error.ResourceNotFoundException;
import lithan.autostrada.auctions.repository.CarListingRepository;
import lithan.autostrada.auctions.repository.ListingTestRideRepository;
import lithan.autostrada.auctions.validation.ImageUploadValidator;
import lithan.autostrada.auctions.validation.ImageUploadValidator.ValidatedImage;

@Service
public class CarListingServiceImpl implements CarListingService {

  @Autowired
  private Clock clock;

  private static final int MAX_VEHICLE_IMAGES = 8;

  private final CarListingRepository listingRepository;
  private final ListingTestRideRepository testRideRepository;
  private final CurrentIdentity currentIdentity;

  public CarListingServiceImpl(
      CarListingRepository listingRepository,
      ListingTestRideRepository testRideRepository,
      CurrentIdentity currentIdentity) {
    this.listingRepository = listingRepository;
    this.testRideRepository = testRideRepository;
    this.currentIdentity = currentIdentity;
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
    return listingRepository.findBySellerIdOrderByCreatedAtDesc(currentIdentity.requireUserId());
  }

  @Override
  public CarListing ownedListing(int listingId) {
    CarListing listing = listingRepository.findById(listingId)
        .orElseThrow(ResourceNotFoundException::new);
    if (listing.getSellerId() != currentIdentity.requireUserId()) {
      throw new AccessDeniedException("You do not own this listing");
    }
    return listing;
  }

  @Override
  @Transactional
  public CarListing create(CarListingForm form, MultipartFile image) {
    return create(form, image == null ? List.of() : List.of(image));
  }

  @Override
  @Transactional
  public CarListing create(CarListingForm form, List<MultipartFile> images) {
    List<ValidatedImage> validatedImages = validateImages(images, 0, true);
    Instant now = Instant.now();
    CarListing listing = new CarListing();
    apply(form, listing);
    listing.setSellerId(currentIdentity.requireUserId());
    listing.setStatus(CarListingStatus.ACTIVE);
    listing.setCreatedAt(now);
    listing.setUpdatedAt(now);
    setPicture(listing, validatedImages.get(0));
    for (int index = 1; index < validatedImages.size(); index++) {
      listing.addGalleryPicture(galleryPicture(validatedImages.get(index), index));
    }
    return listingRepository.save(listing);
  }

  @Override
  @Transactional
  public CarListing update(int listingId, CarListingForm form, MultipartFile image) {
    CarListing listing = editableListing(listingId);
    apply(form, listing);
    if (image != null && !image.isEmpty()) {
      setPicture(listing, validateImage(image));
    }
    listing.setUpdatedAt(Instant.now());
    return listingRepository.save(listing);
  }

  @Override
  @Transactional
  public CarListing update(int listingId, CarListingForm form, List<MultipartFile> images) {
    CarListing listing = editableListing(listingId);
    apply(form, listing);
    int existingCount = listing.getPicture() == null ? 0 : 1;
    existingCount += listing.getGalleryPictures().size();
    List<ValidatedImage> validatedImages = validateImages(images, existingCount, false);
    int displayOrder = listing.getGalleryPictures().size() + 1;
    for (ValidatedImage image : validatedImages) {
      listing.addGalleryPicture(galleryPicture(image, displayOrder++));
    }
    listing.setUpdatedAt(Instant.now());
    return listingRepository.save(listing);
  }

  private CarListing editableListing(int listingId) {
    CarListing listing = ownedListing(listingId);
    if (listing.getStatus() == CarListingStatus.SOLD
        || listing.getStatus() == CarListingStatus.RESERVED) {
      throw new IllegalStateException("Reserved or sold listings cannot be edited");
    }
    return listing;
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
    int requester = currentIdentity.requireUserId();
    if (!listing.isActive()) {
      throw new IllegalStateException("Test rides are only available for active listings");
    }
    if (listing.getSellerId() == requester) {
      throw new IllegalStateException("You cannot schedule a test ride for your own listing");
    }
    if (testRideRepository.existsByUserIdAndListingAndScheduledAt(
        requester, listing, scheduledAt)) {
      throw new IllegalStateException("You already requested this time for the listing");
    }

    Instant now = Instant.now();
    ListingTestRide testRide = new ListingTestRide();
    testRide.setListing(listing);
    testRide.setUserId(requester);
    testRide.setScheduledAt(scheduledAt);
    testRide.setStatus(TestDriveStatus.PENDING);
    testRide.setCreatedAt(now);
    testRide.setUpdatedAt(now);
    testRideRepository.save(testRide);
  }

  @Override
  public List<ListingTestRide> currentUserTestRides() {
    return testRideRepository.findByUserIdOrderByScheduledAtAsc(currentIdentity.requireUserId());
  }

  @Override
  public List<ListingTestRide> testRideRequestsForCurrentSeller() {
    return testRideRepository.findByListingSellerIdOrderByScheduledAtAsc(currentIdentity.requireUserId());
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
    if (testRideRepository.existsByUserIdAndListingAndScheduledAtAndIdTestRideNot(
        testRide.getUserId(), testRide.getListing(), scheduledAt, testRideId)) {
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
    if (testRide.getUserId() != currentIdentity.requireUserId()) {
      throw new AccessDeniedException("This test ride belongs to another user");
    }
    return testRide;
  }

  private ListingTestRide ownedListingTestRide(int testRideId) {
    ListingTestRide testRide = testRideRepository.findById(testRideId)
        .orElseThrow(ResourceNotFoundException::new);
    if (testRide.getListing().getSellerId()
        != currentIdentity.requireUserId()) {
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

  private CarListingGalleryPicture galleryPicture(ValidatedImage image, int displayOrder) {
    CarListingGalleryPicture picture = new CarListingGalleryPicture();
    picture.setFileName(image.fileName());
    picture.setFileType(image.contentType());
    picture.setImage(Base64.getEncoder().encodeToString(image.bytes()));
    picture.setDisplayOrder(displayOrder);
    return picture;
  }

  private List<ValidatedImage> validateImages(
      List<MultipartFile> images,
      int existingCount,
      boolean required) {
    List<MultipartFile> uploads = images == null
        ? List.of()
        : images.stream().filter(image -> image != null && !image.isEmpty()).toList();
    if (required && uploads.isEmpty()) {
      throw new IllegalArgumentException("At least one car picture is required");
    }
    if (existingCount + uploads.size() > MAX_VEHICLE_IMAGES) {
      throw new IllegalArgumentException("A vehicle can have at most 8 pictures");
    }
    return uploads.stream().map(this::validateImage).toList();
  }

  private void validateFuture(LocalDateTime scheduledAt) {
    if (scheduledAt == null || !scheduledAt.isAfter(LocalDateTime.now(clock))) {
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
