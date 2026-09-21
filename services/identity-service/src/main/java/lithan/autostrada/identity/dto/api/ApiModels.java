package lithan.autostrada.identity.dto.api;
import java.util.*;
import jakarta.validation.constraints.*;
public final class ApiModels {
  public record ApiMessageResponse(String message, String redirectUrl) {
  }
  public record ApiErrorResponse(String message, Map<String, String> fieldErrors) {
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
  public record AdminDashboardResponse(
      PageResponse<UserSummaryResponse> users,
      PageResponse<UserSummaryResponse> admins) {
  }
}
