package lithan.autostrada.auctions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ForgotPasswordForm {

  @NotBlank(message = "Enter your email or username")
  @Size(max = 254, message = "Email or username is too long")
  private String identifier;

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }
}
