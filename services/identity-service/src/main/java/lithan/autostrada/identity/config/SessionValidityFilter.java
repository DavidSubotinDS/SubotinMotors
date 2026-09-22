package lithan.autostrada.identity.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Installed only in the browser security chain, after context loading. */
public class SessionValidityFilter extends OncePerRequestFilter {
  private final SessionValidity validity;
  public SessionValidityFilter(SessionValidity validity) { this.validity = validity; }
  @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof CustomUserDetails && validity.current(request) == null)
      SecurityContextHolder.clearContext();
    if (request.getCookies() != null && java.util.Arrays.stream(request.getCookies()).anyMatch(c -> c.getName().equals("JSESSIONID"))) {
      response.addHeader("Set-Cookie", org.springframework.http.ResponseCookie.from("JSESSIONID", "")
          .path("/").httpOnly(true).sameSite("Lax").secure(request.isSecure()).maxAge(0).build().toString());
    }
    chain.doFilter(request, response);
  }
}
