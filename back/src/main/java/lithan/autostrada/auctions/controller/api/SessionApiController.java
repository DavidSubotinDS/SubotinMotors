package lithan.autostrada.auctions.controller.api;

import java.util.List;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lithan.autostrada.auctions.config.CustomUserDetails;
import lithan.autostrada.auctions.dto.api.UserSessionResponse;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.entity.UserProfile;

@RestController
@RequestMapping("/api")
public class SessionApiController {

  @GetMapping("/session")
  public UserSessionResponse session(Authentication authentication) {
    if (authentication == null
        || authentication instanceof AnonymousAuthenticationToken
        || !authentication.isAuthenticated()) {
      return UserSessionResponse.anonymous();
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof CustomUserDetails customUserDetails) {
      UserAccount user = customUserDetails.getUser();
      return new UserSessionResponse(
          true,
          user.getUsername(),
          displayName(user.getProfile(), user.getUsername()),
          roles(authentication));
    }

    return new UserSessionResponse(
        true,
        authentication.getName(),
        authentication.getName(),
        roles(authentication));
  }

  private List<String> roles(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();
  }

  private String displayName(UserProfile profile, String fallback) {
    if (profile == null) {
      return fallback;
    }
    return (profile.getFirstName() + " " + profile.getLastName()).trim();
  }
}
