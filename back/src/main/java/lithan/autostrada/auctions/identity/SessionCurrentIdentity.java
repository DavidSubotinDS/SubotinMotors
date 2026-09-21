package lithan.autostrada.auctions.identity;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import lithan.autostrada.auctions.config.CustomUserDetails;
import lithan.autostrada.auctions.repository.UserRepository;

@Component
public class SessionCurrentIdentity implements CurrentIdentity {
  private final UserRepository users;

  public SessionCurrentIdentity(UserRepository users) { this.users = users; }

  @Override
  public int requireUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
      throw new AccessDeniedException("Sign in to continue");
    }
    if (auth.getPrincipal() instanceof CustomUserDetails principal) {
      if (!users.existsById(principal.getUserId())) {
        throw new AccessDeniedException("Your account is no longer available");
      }
      return principal.getUserId();
    }
    throw new AccessDeniedException("Sign in again to continue");
  }
}
