package lithan.autostrada.identity;

import java.util.Arrays;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;
import lithan.autostrada.identity.config.CustomUserDetails;
import lithan.autostrada.identity.repository.UserRepository;

public final class TestIdentity {
  private TestIdentity() { }
  public static Fixture user(String username) { return new Fixture(username); }
  public record Fixture(String username) {
    public RequestPostProcessor roles(String... roles) {
      return request -> {
        var users = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getServletContext())
            .getBean(UserRepository.class);
        return SecurityMockMvcRequestPostProcessors.authentication(authentication(users, username, roles))
            .postProcessRequest(request);
      };
    }
  }
  static UsernamePasswordAuthenticationToken authentication(UserRepository users, String username, String[] roles) {
    var principal = new CustomUserDetails(users.findByUsername(username).orElseThrow());
    principal.eraseCredentials();
    return UsernamePasswordAuthenticationToken.authenticated(principal, null,
        Arrays.stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());
  }
}
