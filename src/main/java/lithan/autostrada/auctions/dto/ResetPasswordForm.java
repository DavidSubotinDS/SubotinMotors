package lithan.autostrada.auctions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordForm {

  @NotBlank
  private String token;

  @NotBlank(message = "Password is required")
  @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
  private String password;

  @NotBlank(message = "Confirm your password")
  private String confirmPassword;

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getConfirmPassword() {
    return confirmPassword;
  }

  public void setConfirmPassword(String confirmPassword) {
    this.confirmPassword = confirmPassword;
  }
}
