package lithan.autostrada.auctions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ListingCommentForm {

  @NotBlank(message = "Write a comment before posting")
  @Size(max = 1000, message = "Comments must not exceed 1000 characters")
  private String body;

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }
}
