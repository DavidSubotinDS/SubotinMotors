package lithan.autostrada.auctions.dto.api;

import java.util.List;

public record UserSessionResponse(
    boolean authenticated,
    String username,
    String displayName,
    List<String> roles) {

  public static UserSessionResponse anonymous() {
    return new UserSessionResponse(false, null, null, List.of());
  }
}
