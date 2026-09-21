package lithan.autostrada.auctions.identity;

import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;

@Component
public class AssertionCurrentIdentity implements CurrentIdentity {
  @Override public int requireUserId() {
    var auth=SecurityContextHolder.getContext().getAuthentication();
    if(auth instanceof JwtAuthenticationToken jwt && auth.isAuthenticated()
        && "user".equals(jwt.getToken().getClaimAsString("tokenUse"))) {
      try { int id=Integer.parseInt(jwt.getToken().getSubject()); if(id>0) return id; }
      catch(NumberFormatException ignored) { }
    }
    throw new AccessDeniedException("Sign in again to continue");
  }
}
