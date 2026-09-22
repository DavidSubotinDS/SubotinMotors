package lithan.autostrada.auctions;

import java.util.Arrays;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.context.support.WebApplicationContextUtils;
import lithan.autostrada.auctions.config.CustomUserDetails;
import fixtures.identity.repository.UserRepository;

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
  static org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken authentication(UserRepository users, String username, String[] roles) {
    int id=users.findByUsername(username).orElseThrow().getIdUser();
    var jwt=org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-only").header("alg","RS256")
        .subject(Integer.toString(id)).claim("tokenUse","user").claim("roles",Arrays.stream(roles).map(r->"ROLE_"+r).toList())
        .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(60)).build();
    return new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt,
        Arrays.stream(roles).map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());
  }
}
