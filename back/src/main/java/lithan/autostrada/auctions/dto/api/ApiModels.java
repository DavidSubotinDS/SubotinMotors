package lithan.autostrada.auctions.dto.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ApiModels {

  private ApiModels() {
  }

  public record ApiMessageResponse(String message, String redirectUrl) {
  }

  public record ApiErrorResponse(String message, Map<String, String> fieldErrors) {
  }

  public record CheckoutResponse(String checkoutUrl) {
  }

  public record RegistrationRequest(
      String username,
      String email,
      String password,
      String firstName,
      String lastName,
      String phoneNumber,
      String address,
      String streetAddress,
      String city,
      String postalCode,
      String country,
      String about) {
  }

  public record LoginRequest(String username, String password) {
  }

  public record PasswordResetRequest(String identifier) {
  }

  public record PasswordResetCompleteRequest(
      String token,
      String password,
      String confirmPassword) {
  }

  public record ProfileRequest(
      String email,
      String firstName,
      String lastName,
      String phoneNumber,
      String address,
      String streetAddress,
      String city,
      String postalCode,
      String country,
      String about) {
  }

  public record AuctionRequest(
      String make,
      String model,
      String year,
      Integer price,
      LocalDateTime auctionEndTime) {
  }

  public record BidRequest(Integer bidPrice) {
  }

  public record TestDriveRequest(LocalDate date) {
  }

  public record ListingTestRideRequest(LocalDateTime scheduledAt) {
  }

  public record CartItemRequest(Integer idPart, Integer quantity) {
  }

  public record CartQuantityRequest(Integer quantity) {
  }

  public record PartActiveRequest(boolean active) {
  }

  public record ProfileResponse(
      int idProfile,
      Integer idUser,
      String username,
      String email,
      String firstName,
      String lastName,
      String phoneNumber,
      String address,
      String streetAddress,
      String city,
      String postalCode,
      String country,
      String about,
      boolean hasCompleteShippingAddress,
      String formattedShippingAddress,
      String displayLocation,
      String pictureDataUrl) {
  }

  public record UserSummaryResponse(
      int idUser,
      String username,
      String email,
      ProfileResponse profile,
      List<String> roles) {
  }

  public record CommentResponse(
      int idComment,
      String authorName,
      String body,
      String createdAtDisplay,
      String badgeLabel,
      String highlightClass,
      String imageDataUrl) {
  }

  public record AuctionDetailResponse(
      AuctionSummaryResponse auction,
      int highestBid,
      boolean following,
      List<CommentResponse> comments) {
  }

  public record ListingDetailResponse(
      ListingSummaryResponse listing,
      String description,
      boolean stripeEnabled,
      List<CommentResponse> comments) {
  }

  public record PartDetailResponse(
      PartSummaryResponse part,
      boolean active,
      List<CommentResponse> comments) {
  }

  public record BidResponse(
      int idBid,
      int bidPrice,
      String status,
      AuctionSummaryResponse auction,
      UserSummaryResponse bidder) {
  }

  public record TestDriveResponse(
      int idTestDrive,
      LocalDate date,
      String status,
      boolean pending,
      boolean accepted,
      boolean rejected,
      boolean reschedulable,
      boolean cancellable,
      AuctionSummaryResponse auction,
      UserSummaryResponse requester) {
  }

  public record ListingTestRideResponse(
      int idTestRide,
      LocalDateTime scheduledAt,
      String status,
      boolean pending,
      boolean accepted,
      boolean rejected,
      boolean reschedulable,
      boolean cancellable,
      ListingSummaryResponse listing,
      UserSummaryResponse requester) {
  }

  public record AppointmentDashboardResponse(
      List<TestDriveResponse> receivedTestDrives,
      List<TestDriveResponse> bookedTestDrives,
      List<ListingTestRideResponse> listingTestRideRequests,
      List<ListingTestRideResponse> listingTestRides) {
  }

  public record NotificationResponse(
      int idNotification,
      String notificationType,
      String message,
      LocalDateTime createdAt,
      LocalDateTime readAt,
      boolean read,
      AuctionSummaryResponse auction) {
  }

  public record CartItemResponse(
      int idCartItem,
      PartSummaryResponse part,
      int quantity,
      long lineTotalMinor) {
  }

  public record CartResponse(
      List<CartItemResponse> items,
      long totalMinor,
      long itemCount,
      boolean stripeEnabled,
      boolean hasShippingAddress) {
  }

  public record StoreOrderItemResponse(
      int idOrderItem,
      Integer idPart,
      String sku,
      String partName,
      long unitPriceMinor,
      int quantity,
      long lineTotalMinor) {
  }

  public record StoreOrderResponse(
      int idOrder,
      String status,
      String currency,
      long totalMinor,
      String shippingName,
      String shippingAddress,
      String shippingStreetAddress,
      String shippingCity,
      String shippingPostalCode,
      String shippingCountry,
      Instant createdAt,
      Instant updatedAt,
      Instant paidAt,
      UserSummaryResponse user,
      List<StoreOrderItemResponse> items) {
  }

  public record DepositResponse(
      int idDeposit,
      ListingSummaryResponse listing,
      UserSummaryResponse buyer,
      long amountMinor,
      String currency,
      String status,
      Instant createdAt,
      Instant updatedAt,
      Instant paidAt) {
  }

  public record PaymentResponse(
      int idPayment,
      BidResponse bid,
      UserSummaryResponse buyer,
      UserSummaryResponse seller,
      long amountMinor,
      long platformFeeMinor,
      String currency,
      String status,
      String purpose,
      Instant createdAt,
      Instant updatedAt,
      Instant paidAt) {
  }

  public record WebhookEventResponse(
      int idEvent,
      String stripeEventId,
      String eventType,
      String status,
      String message,
      Instant processedAt) {
  }

  public record AdminDashboardResponse(
      PageResponse<UserSummaryResponse> users,
      PageResponse<UserSummaryResponse> admins) {
  }

  public record AdminCarManagementResponse(
      PageResponse<AuctionSummaryResponse> cars,
      PageResponse<BidResponse> bids) {
  }

  public record AdminTransactionsResponse(
      PageResponse<PaymentResponse> transactions,
      List<WebhookEventResponse> webhookEvents) {
  }

  public record UserWorkspaceResponse(
      ProfileResponse profile,
      List<AuctionSummaryResponse> auctions,
      List<ListingSummaryResponse> listings,
      List<BidResponse> bids,
      long unreadNotifications,
      long cartItems) {
  }
}
