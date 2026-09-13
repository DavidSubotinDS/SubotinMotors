package lithan.autostrada.auctions.dto.api;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import lithan.autostrada.auctions.validation.ProductionYear;

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
      @NotBlank(message = "Username is required")
      @Size(min = 3, max = 15, message = "Username must be between 3 and 15 characters long")
      String username,
      @NotBlank(message = "Email is required")
      @Email(message = "Enter a valid email address")
      @Size(max = 254, message = "Email must not exceed 254 characters")
      String email,
      @NotBlank(message = "Password is required")
      @Size(min = 6, message = "Password must be greater or equal to 6")
      String password,
      @NotBlank(message = "First name is required")
      @Size(max = 35, message = "First name must be between 1 and 35 characters long")
      String firstName,
      @NotBlank(message = "Last name is required")
      @Size(max = 35, message = "Last name must be between 1 and 35 characters long")
      String lastName,
      @NotBlank(message = "Phone number is required")
      @Pattern(regexp = "^(?:\\+[1-9]\\d{7,14}|\\d{8,15})$",
          message = "Phone number must contain 8 to 15 digits, with an optional leading +")
      String phoneNumber,
      String address,
      @Size(max = 255, message = "Street address must not exceed 255 characters")
      String streetAddress,
      @Size(max = 120, message = "City must not exceed 120 characters")
      String city,
      @Size(max = 30, message = "Postal code must not exceed 30 characters")
      String postalCode,
      @Size(max = 120, message = "Country must not exceed 120 characters")
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
      @NotBlank(message = "Make is required")
      String make,
      @NotBlank(message = "Model is required")
      String model,
      @ProductionYear
      String year,
      @NotNull(message = "Price is required")
      @Positive(message = "Price must be greater than zero")
      Integer price,
      @NotNull(message = "Auction end date and time is required")
      @Future(message = "Auction end date and time must be in the future")
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
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
