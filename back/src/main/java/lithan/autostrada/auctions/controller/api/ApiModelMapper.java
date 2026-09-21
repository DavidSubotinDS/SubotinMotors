package lithan.autostrada.auctions.controller.api;

import java.util.List;
import java.time.Clock;
import java.time.LocalDateTime;

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
import lithan.autostrada.auctions.entity.StoreOrder;
import lithan.autostrada.auctions.entity.StoreOrderItem;
import lithan.autostrada.auctions.entity.TestDrive;

@Component
public class ApiModelMapper {

  private final Clock clock;
  private final lithan.autostrada.auctions.identity.ProfileClient profiles;
  private final ThreadLocal<java.util.Map<Integer, lithan.autostrada.auctions.identity.PublicProfile>> batch = new ThreadLocal<>();

  public ApiModelMapper(Clock clock, lithan.autostrada.auctions.identity.ProfileClient profiles) {
    this.clock = clock;
    this.profiles = profiles;
  }

  public AuctionSummaryResponse auction(Car car) {
    LocalDateTime now = LocalDateTime.now(clock);
    List<String> imageUrls = new java.util.ArrayList<>();
    if (car.getCarPicture() != null) {
      imageUrls.add(imageDataUrl(
          car.getCarPicture().getFileType(),
          car.getCarPicture().getImage()));
    }
    car.getGalleryPictures().forEach(picture -> imageUrls.add(
        imageDataUrl(picture.getFileType(), picture.getImage())));
    return new AuctionSummaryResponse(
        car.getIdCar(),
        car.getMake(),
        car.getModel(),
        car.getYear(),
        car.getPrice(),
        car.auctionStatusAt(now),
        car.auctionStatusLabelAt(now),
        car.getAuctionEndTimeDisplay(),
        car.getAuctionEndTimeEpochMillis(),
        imageUrls.isEmpty() ? null : imageUrls.get(0),
        List.copyOf(imageUrls),
        displayName(car.getUserId()));
  }

  public ListingSummaryResponse listing(CarListing listing) {
    List<String> imageUrls = new java.util.ArrayList<>();
    if (listing.getPicture() != null) {
      imageUrls.add(imageDataUrl(
          listing.getPicture().getFileType(),
          listing.getPicture().getImage()));
    }
    listing.getGalleryPictures().forEach(picture -> imageUrls.add(
        imageDataUrl(picture.getFileType(), picture.getImage())));
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
        imageUrls.isEmpty() ? null : imageUrls.get(0),
        List.copyOf(imageUrls),
        displayName(listing.getSellerId()));
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
        part.getImageUrl(),
        part.isActive());
  }

  public PartDetailResponse partDetail(CarPart part, List<ListingCommentView> comments) {
    return new PartDetailResponse(
        part(part),
        part.isActive(),
        comments.stream().map(this::comment).toList());
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
        user(bid.getUserId()));
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
        user(testDrive.getUserId()));
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
        user(testRide.getUserId()));
  }

  public AppointmentDashboardResponse appointments(
      List<TestDrive> receivedTestDrives,
      List<TestDrive> bookedTestDrives,
      List<ListingTestRide> listingTestRideRequests,
      List<ListingTestRide> listingTestRides) {
    return new AppointmentDashboardResponse(
        map(receivedTestDrives.stream(), this::testDrive).toList(),
        map(bookedTestDrives.stream(), this::testDrive).toList(),
        map(listingTestRideRequests.stream(), this::listingTestRide).toList(),
        map(listingTestRides.stream(), this::listingTestRide).toList());
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
        user(order.getUserId()),
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
        user(deposit.getBuyerId()),
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
        user(payment.getBuyerId()),
        user(payment.getSellerId()),
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

  private lithan.autostrada.auctions.identity.PublicProfile lookup(int id) {
    var current = batch.get();
    return current == null ? profiles.display(id)
        : current.getOrDefault(id, lithan.autostrada.auctions.identity.PublicProfile.missing(id));
  }

  private String displayName(int id) { return lookup(id).displayName(); }

  public ProfileResponse publicProfile(lithan.autostrada.auctions.identity.PublicProfile p) {
    return publicProfile(p, false);
  }

  private ProfileResponse publicProfile(lithan.autostrada.auctions.identity.PublicProfile p, boolean nestedUser) {
    if (p.profileId() == null) return null;
    return new ProfileResponse(p.profileId(), nestedUser ? p.userId() : null, nestedUser ? p.username() : null, null,
        p.firstName(), p.lastName(), null, null, null, p.city(), null, p.country(),
        p.about(), false, "", p.displayLocation(), imageDataUrl(p.pictureType(), p.picture()));
  }

  private UserSummaryResponse user(int id) {
    var p = lookup(id);
    return new UserSummaryResponse(id, p.username(), null, publicProfile(p, true), List.of());
  }

  public <T, R> org.springframework.data.domain.Page<R> map(
      org.springframework.data.domain.Page<T> source, java.util.function.Function<T, R> mapping) {
    return withProfiles(source.getContent(), () -> source.map(mapping));
  }

  public <T, R> java.util.stream.Stream<R> map(
      java.util.stream.Stream<T> source, java.util.function.Function<T, R> mapping) {
    var values = source.toList();
    return withProfiles(values, () -> values.stream().map(mapping).toList()).stream();
  }

  private <T> T withProfiles(java.util.Collection<?> values, java.util.function.Supplier<T> mapping) {
    var ids = new java.util.HashSet<Integer>();
    values.forEach(value -> collectIds(value, ids));
    var previous = batch.get();
    batch.set(profiles.findAll(ids));
    try { return mapping.get(); }
    finally { if (previous == null) batch.remove(); else batch.set(previous); }
  }

  private void collectIds(Object value, java.util.Set<Integer> ids) {
    if (value instanceof Car car) ids.add(car.getUserId());
    else if (value instanceof CarListing listing) ids.add(listing.getSellerId());
    else if (value instanceof CarBidding bid) { ids.add(bid.getUserId()); collectIds(bid.getCar(), ids); }
    else if (value instanceof TestDrive ride) { ids.add(ride.getUserId()); collectIds(ride.getCar(), ids); }
    else if (value instanceof ListingTestRide ride) { ids.add(ride.getUserId()); collectIds(ride.getListing(), ids); }
    else if (value instanceof AuctionNotification notification) collectIds(notification.getCar(), ids);
    else if (value instanceof StoreOrder order) ids.add(order.getUserId());
    else if (value instanceof ListingDeposit deposit) { ids.add(deposit.getBuyerId()); collectIds(deposit.getListing(), ids); }
    else if (value instanceof PaymentOrder payment) {
      ids.add(payment.getBuyerId()); ids.add(payment.getSellerId()); collectIds(payment.getBid(), ids);
    }
  }
}
