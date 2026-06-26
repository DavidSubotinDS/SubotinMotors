package lithan.autostrada.auctions.controller.api;

import java.time.LocalDateTime;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lithan.autostrada.auctions.dto.CarListingForm;
import lithan.autostrada.auctions.dto.UserProfileForm;
import lithan.autostrada.auctions.dto.api.ApiModels.ApiMessageResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.AppointmentDashboardResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.AuctionRequest;
import lithan.autostrada.auctions.dto.api.ApiModels.BidRequest;
import lithan.autostrada.auctions.dto.api.ApiModels.BidResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.CheckoutResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.DepositResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.ListingTestRideRequest;
import lithan.autostrada.auctions.dto.api.ApiModels.NotificationResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.ProfileRequest;
import lithan.autostrada.auctions.dto.api.ApiModels.ProfileResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.TestDriveRequest;
import lithan.autostrada.auctions.dto.api.ApiModels.UserWorkspaceResponse;
import lithan.autostrada.auctions.dto.api.AuctionSummaryResponse;
import lithan.autostrada.auctions.dto.api.ListingSummaryResponse;
import lithan.autostrada.auctions.dto.api.PageResponse;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.service.AuctionFollowService;
import lithan.autostrada.auctions.service.AuctionNotificationService;
import lithan.autostrada.auctions.service.CarListingService;
import lithan.autostrada.auctions.service.CartService;
import lithan.autostrada.auctions.service.ListingDepositService;
import lithan.autostrada.auctions.service.UserCarService;
import lithan.autostrada.auctions.service.UserService;

@RestController
@RequestMapping("/api/user")
public class UserWorkspaceApiController {

  private final UserService userService;
  private final UserCarService userCarService;
  private final CarListingService listingService;
  private final ListingDepositService depositService;
  private final AuctionFollowService followService;
  private final AuctionNotificationService notificationService;
  private final CartService cartService;
  private final ApiModelMapper mapper;

  public UserWorkspaceApiController(
      UserService userService,
      UserCarService userCarService,
      CarListingService listingService,
      ListingDepositService depositService,
      AuctionFollowService followService,
      AuctionNotificationService notificationService,
      CartService cartService,
      ApiModelMapper mapper) {
    this.userService = userService;
    this.userCarService = userCarService;
    this.listingService = listingService;
    this.depositService = depositService;
    this.followService = followService;
    this.notificationService = notificationService;
    this.cartService = cartService;
    this.mapper = mapper;
  }

  @GetMapping("/workspace")
  public UserWorkspaceResponse workspace() {
    UserAccount user = userService.getUserLogin();
    return new UserWorkspaceResponse(
        mapper.profile(user),
        userCarService.listUserCar().stream().map(mapper::auction).toList(),
        listingService.currentUserListings().stream().map(mapper::listing).toList(),
        userCarService.listCurrentUserBids().stream().map(mapper::bid).toList(),
        notificationService.unreadCount(),
        cartService.itemCount());
  }

  @GetMapping("/profile")
  public ProfileResponse profile() {
    return mapper.profile(userService.getUserLogin());
  }

  @PutMapping("/profile")
  public ProfileResponse updateProfile(
      @RequestBody ProfileRequest request,
      HttpSession session) {
    UserProfileForm form = new UserProfileForm();
    form.setIdProfile(userService.getUserLogin().getProfile().getIdProfile());
    form.setEmail(request.email());
    form.setFirstName(request.firstName());
    form.setLastName(request.lastName());
    form.setPhoneNumber(request.phoneNumber());
    form.setAddress(request.address());
    form.setStreetAddress(request.streetAddress());
    form.setCity(request.city());
    form.setPostalCode(request.postalCode());
    form.setCountry(request.country());
    form.setAbout(request.about());
    try {
      userService.editUserProfile(form);
    } catch (DataIntegrityViolationException exception) {
      throw new IllegalArgumentException("That email is already registered.");
    }
    UserAccount user = userService.getUserLogin();
    session.setAttribute("profileLog", user.getProfile());
    return mapper.profile(user);
  }

  @PostMapping("/profile/picture")
  public ProfileResponse updateProfilePicture(
      @RequestParam("imageFile") MultipartFile imageFile,
      HttpSession session) throws Exception {
    UserAccount user = userService.getUserLogin();
    userService.saveImage(imageFile, user.getProfile());
    session.setAttribute("profileLog", user.getProfile());
    return mapper.profile(userService.getUserLogin());
  }

  @GetMapping("/auctions")
  public java.util.List<AuctionSummaryResponse> auctions() {
    return userCarService.listUserCar().stream().map(mapper::auction).toList();
  }

  @GetMapping("/auctions/{idCar}")
  public AuctionSummaryResponse ownedAuction(@PathVariable int idCar) {
    return mapper.auction(userCarService.getOwnedCarById(idCar));
  }

  @PostMapping("/auctions")
  public AuctionSummaryResponse createAuction(
      @Valid @ModelAttribute AuctionRequest request,
      @RequestParam("imageFile") MultipartFile imageFile) throws Exception {
    Car car = toCar(request);
    if (imageFile.isEmpty()) {
      throw new IllegalArgumentException("Car picture is required.");
    }
    userCarService.postCar(imageFile, car);
    return mapper.auction(car);
  }

  @PutMapping("/auctions/{idCar}")
  public AuctionSummaryResponse updateAuction(
      @PathVariable int idCar,
      @RequestBody AuctionRequest request) {
    Car car = toCar(request);
    car.setIdCar(idCar);
    return mapper.auction(userCarService.editOwnedCar(car));
  }

  @PostMapping("/auctions/{idCar}/picture")
  public ApiMessageResponse uploadAuctionPicture(
      @PathVariable int idCar,
      @RequestParam("imageFile") MultipartFile imageFile) throws Exception {
    userCarService.saveUploadPicture(imageFile, idCar);
    return new ApiMessageResponse("Auction picture updated.", null);
  }

  @PostMapping("/auctions/{idCar}/activate")
  public ApiMessageResponse activateAuction(@PathVariable int idCar) {
    userCarService.changeOwnedCarStatus(idCar, "ACTIVE");
    return new ApiMessageResponse("Auction activated.", null);
  }

  @PostMapping("/auctions/{idCar}/deactivate")
  public ApiMessageResponse deactivateAuction(@PathVariable int idCar) {
    userCarService.changeOwnedCarStatus(idCar, "DEACTIVE");
    return new ApiMessageResponse("Auction deactivated.", null);
  }

  @PostMapping("/auctions/{idCar}/bid")
  public ApiMessageResponse bid(
      @PathVariable int idCar,
      @RequestBody BidRequest request) {
    if (request.bidPrice() == null) {
      throw new IllegalArgumentException("Bid price is required.");
    }
    userCarService.placeBid(idCar, request.bidPrice());
    return new ApiMessageResponse("Bid placed.", null);
  }

  @PostMapping("/auctions/{idCar}/test-drives")
  public ApiMessageResponse scheduleTestDrive(
      @PathVariable int idCar,
      @RequestBody TestDriveRequest request) {
    userCarService.saveTestDrive(request.date(), idCar);
    return new ApiMessageResponse("Test drive requested.", null);
  }

  @PostMapping("/auctions/{idCar}/follow")
  public ApiMessageResponse followAuction(@PathVariable int idCar) {
    boolean created = followService.follow(idCar);
    return new ApiMessageResponse(
        created ? "Auction added to your watchlist." : "You already follow this auction.",
        null);
  }

  @PostMapping("/auctions/{idCar}/unfollow")
  public ApiMessageResponse unfollowAuction(@PathVariable int idCar) {
    followService.unfollow(idCar);
    return new ApiMessageResponse("Auction removed from your watchlist.", null);
  }

  @GetMapping("/followed-auctions")
  public java.util.List<AuctionSummaryResponse> followedAuctions() {
    return followService.listCurrentUserFollows().stream()
        .map(follow -> mapper.auction(follow.getCar()))
        .toList();
  }

  @GetMapping("/notifications")
  public java.util.List<NotificationResponse> notifications() {
    return notificationService.listCurrentUserNotifications().stream()
        .map(mapper::notification)
        .toList();
  }

  @PostMapping("/notifications/{notificationId}/read")
  public ApiMessageResponse markNotificationRead(@PathVariable int notificationId) {
    notificationService.markRead(notificationId);
    return new ApiMessageResponse("Notification marked read.", null);
  }

  @PostMapping("/notifications/read-all")
  public ApiMessageResponse markAllNotificationsRead() {
    notificationService.markAllRead();
    return new ApiMessageResponse("All notifications marked read.", null);
  }

  @GetMapping("/bids")
  public java.util.List<BidResponse> bids() {
    return userCarService.listCurrentUserBids().stream().map(mapper::bid).toList();
  }

  @PostMapping("/bids/{idBid}/cancel")
  public ApiMessageResponse cancelBid(@PathVariable int idBid) {
    userCarService.cancelCurrentUserBid(idBid);
    return new ApiMessageResponse("Bid cancelled.", null);
  }

  @GetMapping("/appointments")
  public AppointmentDashboardResponse appointments() {
    return mapper.appointments(
        userCarService.listTestDriveForOwnedCars(),
        userCarService.listCurrentUserTestDrives(),
        listingService.testRideRequestsForCurrentSeller(),
        listingService.currentUserTestRides());
  }

  @PostMapping("/test-drives/{idTestDrive}/reschedule")
  public ApiMessageResponse rescheduleTestDrive(
      @PathVariable int idTestDrive,
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
    userCarService.rescheduleCurrentUserTestDrive(idTestDrive, date);
    return new ApiMessageResponse("Test drive rescheduled.", null);
  }

  @PostMapping("/test-drives/{idTestDrive}/cancel")
  public ApiMessageResponse cancelTestDrive(@PathVariable int idTestDrive) {
    userCarService.cancelCurrentUserTestDrive(idTestDrive);
    return new ApiMessageResponse("Test drive cancelled.", null);
  }

  @PostMapping("/test-drives/{idTestDrive}/accept")
  public ApiMessageResponse acceptTestDrive(@PathVariable int idTestDrive) {
    userCarService.acceptTestDriveForOwnedCar(idTestDrive);
    return new ApiMessageResponse("Test-drive request accepted.", null);
  }

  @PostMapping("/test-drives/{idTestDrive}/reject")
  public ApiMessageResponse rejectTestDrive(@PathVariable int idTestDrive) {
    userCarService.rejectTestDriveForOwnedCar(idTestDrive);
    return new ApiMessageResponse("Test-drive request rejected.", null);
  }

  @PostMapping("/test-drives/{idTestDrive}/owner-cancel")
  public ApiMessageResponse cancelOwnedTestDrive(@PathVariable int idTestDrive) {
    userCarService.cancelTestDriveForOwnedCar(idTestDrive);
    return new ApiMessageResponse("Accepted test drive cancelled.", null);
  }

  @GetMapping("/listings")
  public java.util.List<ListingSummaryResponse> listings() {
    return listingService.currentUserListings().stream().map(mapper::listing).toList();
  }

  @GetMapping("/listings/{listingId}")
  public CarListingForm ownedListing(@PathVariable int listingId) {
    return CarListingForm.from(listingService.ownedListing(listingId));
  }

  @PostMapping("/listings")
  public ListingSummaryResponse createListing(
      @Valid @ModelAttribute CarListingForm form,
      @RequestPart("imageFile") MultipartFile image) {
    return mapper.listing(listingService.create(form, image));
  }

  @PutMapping("/listings/{listingId}")
  public ListingSummaryResponse updateListing(
      @PathVariable int listingId,
      @Valid @ModelAttribute CarListingForm form,
      @RequestPart(name = "imageFile", required = false) MultipartFile image) {
    return mapper.listing(listingService.update(listingId, form, image));
  }

  @PostMapping("/listings/{listingId}/activate")
  public ApiMessageResponse activateListing(@PathVariable int listingId) {
    listingService.activate(listingId);
    return new ApiMessageResponse("Listing activated.", null);
  }

  @PostMapping("/listings/{listingId}/deactivate")
  public ApiMessageResponse deactivateListing(@PathVariable int listingId) {
    listingService.deactivate(listingId);
    return new ApiMessageResponse("Listing deactivated.", null);
  }

  @PostMapping("/listings/{listingId}/deposit")
  public CheckoutResponse startListingDeposit(@PathVariable int listingId) {
    return new CheckoutResponse(depositService.startCheckout(listingId));
  }

  @PostMapping("/listings/{listingId}/test-rides")
  public ApiMessageResponse scheduleListingTestRide(
      @PathVariable int listingId,
      @RequestBody ListingTestRideRequest request) {
    listingService.scheduleTestRide(listingId, request.scheduledAt());
    return new ApiMessageResponse("Test ride request sent to the seller.", null);
  }

  @PostMapping("/listing-test-rides/{idTestRide}/reschedule")
  public ApiMessageResponse rescheduleListingTestRide(
      @PathVariable int idTestRide,
      @RequestParam("scheduledAt")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledAt) {
    listingService.rescheduleCurrentUserTestRide(idTestRide, scheduledAt);
    return new ApiMessageResponse("Test ride rescheduled.", null);
  }

  @PostMapping("/listing-test-rides/{idTestRide}/cancel")
  public ApiMessageResponse cancelListingTestRide(@PathVariable int idTestRide) {
    listingService.cancelCurrentUserTestRide(idTestRide);
    return new ApiMessageResponse("Test ride cancelled.", null);
  }

  @PostMapping("/listing-test-rides/{idTestRide}/accept")
  public ApiMessageResponse acceptListingTestRide(@PathVariable int idTestRide) {
    listingService.acceptTestRideForOwnedListing(idTestRide);
    return new ApiMessageResponse("Test ride request accepted.", null);
  }

  @PostMapping("/listing-test-rides/{idTestRide}/reject")
  public ApiMessageResponse rejectListingTestRide(@PathVariable int idTestRide) {
    listingService.rejectTestRideForOwnedListing(idTestRide);
    return new ApiMessageResponse("Test ride request rejected.", null);
  }

  @PostMapping("/listing-test-rides/{idTestRide}/owner-cancel")
  public ApiMessageResponse cancelOwnedListingTestRide(@PathVariable int idTestRide) {
    listingService.cancelTestRideForOwnedListing(idTestRide);
    return new ApiMessageResponse("Accepted test ride cancelled.", null);
  }

  @GetMapping("/listing-deposits")
  public PageResponse<DepositResponse> listingDeposits(
      @RequestParam(defaultValue = "0") int page) {
    var deposits = depositService.currentUserDeposits(
        org.springframework.data.domain.PageRequest.of(
            Math.max(page, 0),
            10,
            org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.DESC,
                "createdAt")));
    return PageResponse.from(deposits.map(mapper::deposit));
  }

  @GetMapping("/listing-deposits/success")
  public DepositResponse listingDepositSuccess(@RequestParam("session_id") String sessionId) {
    return mapper.deposit(depositService.currentUserDepositBySession(sessionId));
  }

  private Car toCar(AuctionRequest request) {
    Car car = new Car();
    car.setMake(request.make());
    car.setModel(request.model());
    car.setYear(request.year());
    car.setPrice(request.price() == null ? 0 : request.price());
    car.setAuctionEndTime(request.auctionEndTime());
    return car;
  }
}
