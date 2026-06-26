package lithan.autostrada.auctions.controller.api;

import java.util.List;

import org.springframework.stereotype.Component;

import lithan.autostrada.auctions.dto.ListingCommentView;
import lithan.autostrada.auctions.dto.api.ApiModels.AdminCarManagementResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.AdminDashboardResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.AdminTransactionsResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.AppointmentDashboardResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.BidResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.CartItemResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.CartResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.CommentResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.DepositResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.ListingDetailResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.ListingTestRideResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.NotificationResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.PartDetailResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.PaymentResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.ProfileResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.StoreOrderItemResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.StoreOrderResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.TestDriveResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.UserSummaryResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.WebhookEventResponse;
import lithan.autostrada.auctions.dto.api.AuctionSummaryResponse;
import lithan.autostrada.auctions.dto.api.ListingSummaryResponse;
import lithan.autostrada.auctions.dto.api.PageResponse;
import lithan.autostrada.auctions.dto.api.PartSummaryResponse;
import lithan.autostrada.auctions.entity.AuctionNotification;
import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.CarBidding;
import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.CartItem;
import lithan.autostrada.auctions.entity.ListingDeposit;
import lithan.autostrada.auctions.entity.ListingTestRide;
import lithan.autostrada.auctions.entity.PaymentOrder;
import lithan.autostrada.auctions.entity.PaymentWebhookEvent;
import lithan.autostrada.auctions.entity.ProfilePicture;
import lithan.autostrada.auctions.entity.Role;
import lithan.autostrada.auctions.entity.StoreOrder;
import lithan.autostrada.auctions.entity.StoreOrderItem;
import lithan.autostrada.auctions.entity.TestDrive;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.entity.UserProfile;

@Component
public class ApiModelMapper {

  public AuctionSummaryResponse auction(Car car) {
    return new AuctionSummaryResponse(
        car.getIdCar(),
        car.getMake(),
        car.getModel(),
        car.getYear(),
        car.getPrice(),
        car.getAuctionStatus(),
        car.getAuctionStatusLabel(),
        car.getAuctionEndTimeDisplay(),
        car.getAuctionEndTimeEpochMillis(),
        car.getCarPicture() == null
            ? null
            : imageDataUrl(car.getCarPicture().getFileType(), car.getCarPicture().getImage()),
        displayName(car.getUser()));
  }

  public ListingSummaryResponse listing(CarListing listing) {
    return new ListingSummaryResponse(
        listing.getIdListing(),
        listing.getTitle(),
        listing.getMake(),
        listing.getModel(),
        listing.getYear(),
        listing.getMileage(),
        listing.getFuelType(),
        listing.getTransmission(),
        listing.getPriceMinor(),
        listing.getDepositAmountMinor(),
        listing.getStatus().name(),
        listing.getPicture() == null
            ? null
            : imageDataUrl(listing.getPicture().getFileType(), listing.getPicture().getImage()),
        displayName(listing.getSeller()));
  }

  public ListingDetailResponse listingDetail(
      CarListing listing,
      boolean stripeEnabled,
      List<ListingCommentView> comments) {
    return new ListingDetailResponse(
        listing(listing),
        listing.getDescription(),
        stripeEnabled,
        comments.stream().map(this::comment).toList());
  }

  public PartSummaryResponse part(CarPart part) {
    return new PartSummaryResponse(
        part.getIdPart(),
        part.getSku(),
        part.getName(),
        part.getCategory(),
        part.getDescription(),
        part.getPriceMinor(),
        part.getStockQuantity(),
        part.getImageUrl());
  }

  public PartDetailResponse partDetail(CarPart part, List<ListingCommentView> comments) {
    return new PartDetailResponse(
        part(part),
        part.isActive(),
        comments.stream().map(this::comment).toList());
  }

  public ProfileResponse profile(UserAccount user) {
    return profile(user.getProfile(), user);
  }

  public ProfileResponse profile(UserProfile profile, UserAccount user) {
    ProfilePicture picture = profile.getProfilePicture();
    return new ProfileResponse(
        profile.getIdProfile(),
        user == null ? null : user.getIdUser(),
        user == null ? null : user.getUsername(),
        user == null ? null : user.getEmail(),
        profile.getFirstName(),
        profile.getLastName(),
        profile.getPhoneNumber(),
        profile.getAddress(),
        profile.getStreetAddress(),
        profile.getCity(),
        profile.getPostalCode(),
        profile.getCountry(),
        profile.getAbout(),
        profile.hasCompleteShippingAddress(),
        profile.getFormattedShippingAddress(),
        profile.getDisplayLocation(),
        picture == null ? null : imageDataUrl(picture.getFileType(), picture.getImage()));
  }

  public UserSummaryResponse user(UserAccount user) {
    return new UserSummaryResponse(
        user.getIdUser(),
        user.getUsername(),
        user.getEmail(),
        user.getProfile() == null ? null : profile(user),
        user.getRoles() == null
            ? List.of()
            : user.getRoles().stream().map(Role::getRole).toList());
  }

  public CommentResponse comment(ListingCommentView comment) {
    return new CommentResponse(
        comment.getIdComment(),
        comment.getAuthorName(),
        comment.getBody(),
        comment.getCreatedAtDisplay(),
        comment.getBadgeLabel(),
        comment.getHighlightClass(),
        comment.isHasImage()
            ? imageDataUrl(comment.getImageFileType(), comment.getImageData())
            : null);
  }

  public BidResponse bid(CarBidding bid) {
    return new BidResponse(
        bid.getIdBid(),
        bid.getBidPrice(),
        bid.getStatus(),
        auction(bid.getCar()),
        user(bid.getUser()));
  }

  public TestDriveResponse testDrive(TestDrive testDrive) {
    return new TestDriveResponse(
        testDrive.getIdTestDrive(),
        testDrive.getDate(),
        testDrive.getStatus().name(),
        testDrive.isPending(),
        testDrive.isAccepted(),
        testDrive.isRejected(),
        testDrive.isReschedulable(),
        testDrive.isCancellable(),
        auction(testDrive.getCar()),
        user(testDrive.getUser()));
  }

  public ListingTestRideResponse listingTestRide(ListingTestRide testRide) {
    return new ListingTestRideResponse(
        testRide.getIdTestRide(),
        testRide.getScheduledAt(),
        testRide.getStatus().name(),
        testRide.isPending(),
        testRide.isAccepted(),
        testRide.isRejected(),
        testRide.isReschedulable(),
        testRide.isCancellable(),
        listing(testRide.getListing()),
        user(testRide.getUser()));
  }

  public AppointmentDashboardResponse appointments(
      List<TestDrive> receivedTestDrives,
      List<TestDrive> bookedTestDrives,
      List<ListingTestRide> listingTestRideRequests,
      List<ListingTestRide> listingTestRides) {
    return new AppointmentDashboardResponse(
        receivedTestDrives.stream().map(this::testDrive).toList(),
        bookedTestDrives.stream().map(this::testDrive).toList(),
        listingTestRideRequests.stream().map(this::listingTestRide).toList(),
        listingTestRides.stream().map(this::listingTestRide).toList());
  }

  public NotificationResponse notification(AuctionNotification notification) {
    return new NotificationResponse(
        notification.getIdNotification(),
        notification.getNotificationType(),
        notification.getMessage(),
        notification.getCreatedAt(),
        notification.getReadAt(),
        notification.isRead(),
        auction(notification.getCar()));
  }

  public CartItemResponse cartItem(CartItem item) {
    return new CartItemResponse(
        item.getIdCartItem(),
        part(item.getPart()),
        item.getQuantity(),
        item.getLineTotalMinor());
  }

  public CartResponse cart(
      List<CartItem> items,
      long totalMinor,
      long itemCount,
      boolean stripeEnabled,
      boolean hasShippingAddress) {
    return new CartResponse(
        items.stream().map(this::cartItem).toList(),
        totalMinor,
        itemCount,
        stripeEnabled,
        hasShippingAddress);
  }

  public StoreOrderResponse storeOrder(StoreOrder order) {
    return new StoreOrderResponse(
        order.getIdOrder(),
        order.getStatus(),
        order.getCurrency(),
        order.getTotalMinor(),
        order.getShippingName(),
        order.getShippingAddress(),
        order.getShippingStreetAddress(),
        order.getShippingCity(),
        order.getShippingPostalCode(),
        order.getShippingCountry(),
        order.getCreatedAt(),
        order.getUpdatedAt(),
        order.getPaidAt(),
        user(order.getUser()),
        order.getItems().stream().map(this::storeOrderItem).toList());
  }

  public StoreOrderItemResponse storeOrderItem(StoreOrderItem item) {
    return new StoreOrderItemResponse(
        item.getIdOrderItem(),
        item.getPart() == null ? null : item.getPart().getIdPart(),
        item.getSku(),
        item.getPartName(),
        item.getUnitPriceMinor(),
        item.getQuantity(),
        item.getLineTotalMinor());
  }

  public DepositResponse deposit(ListingDeposit deposit) {
    return new DepositResponse(
        deposit.getIdDeposit(),
        listing(deposit.getListing()),
        user(deposit.getBuyer()),
        deposit.getAmountMinor(),
        deposit.getCurrency(),
        deposit.getStatus(),
        deposit.getCreatedAt(),
        deposit.getUpdatedAt(),
        deposit.getPaidAt());
  }

  public PaymentResponse payment(PaymentOrder payment) {
    return new PaymentResponse(
        payment.getIdPayment(),
        bid(payment.getBid()),
        user(payment.getBuyer()),
        user(payment.getSeller()),
        payment.getAmountMinor(),
        payment.getPlatformFeeMinor(),
        payment.getCurrency(),
        payment.getStatus(),
        payment.getPurpose(),
        payment.getCreatedAt(),
        payment.getUpdatedAt(),
        payment.getPaidAt());
  }

  public WebhookEventResponse webhook(PaymentWebhookEvent event) {
    return new WebhookEventResponse(
        event.getIdWebhookEvent(),
        event.getProviderEventId(),
        event.getEventType(),
        "PROCESSED",
        null,
        event.getProcessedAt());
  }

  public AdminDashboardResponse adminDashboard(
      PageResponse<UserSummaryResponse> users,
      PageResponse<UserSummaryResponse> admins) {
    return new AdminDashboardResponse(users, admins);
  }

  public AdminCarManagementResponse adminCarManagement(
      PageResponse<AuctionSummaryResponse> cars,
      PageResponse<BidResponse> bids) {
    return new AdminCarManagementResponse(cars, bids);
  }

  public AdminTransactionsResponse adminTransactions(
      PageResponse<PaymentResponse> transactions,
      List<PaymentWebhookEvent> webhookEvents) {
    return new AdminTransactionsResponse(
        transactions,
        webhookEvents.stream().map(this::webhook).toList());
  }

  public String imageDataUrl(String fileType, String image) {
    if (fileType == null || fileType.isBlank() || image == null || image.isBlank()) {
      return null;
    }
    return "data:" + fileType + ";base64," + image;
  }

  private String displayName(UserAccount user) {
    if (user == null) {
      return "";
    }
    UserProfile profile = user.getProfile();
    if (profile == null) {
      return user.getUsername();
    }
    String fullName = (profile.getFirstName() + " " + profile.getLastName()).trim();
    return fullName.isBlank() ? user.getUsername() : fullName;
  }
}
