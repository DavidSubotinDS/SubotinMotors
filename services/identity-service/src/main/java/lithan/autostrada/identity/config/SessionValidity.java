package lithan.autostrada.identity.config;

import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import lithan.autostrada.identity.repository.UserRepository;

/** Rechecks account security state for every admitted request, including exchange. */
@Component
public class SessionValidity {
  private final UserRepository users;
  public SessionValidity(UserRepository users) { this.users = users; }
  public Authentication current(HttpServletRequest request) {
    var session = request.getSession(false);
    if (session == null) return null;
    Object value = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    if (!(value instanceof SecurityContext context)) return null;
    var authentication = context.getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) return null;
    var password = users.findPasswordHashByIdUser(principal.getUserId());
    var roles = users.findRoleNamesByIdUser(principal.getUserId());
    if (password.isEmpty() || !principal.matchesSecurityState(password.get(), roles)
        || principal.getAuthenticatedAt().plusSeconds(8 * 60 * 60).isBefore(java.time.Instant.now())) {
      session.invalidate();
      return null;
    }
    return authentication;
  }
}
