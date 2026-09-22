package lithan.autostrada.identity.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import lithan.autostrada.identity.config.*;
import lithan.autostrada.identity.identity.*;
import lithan.autostrada.identity.repository.UserRepository;

@RestController @RequestMapping("/internal/v1")
public class InternalIdentityController {
  private final IdentityTokens tokens; private final SessionValidity sessions;
  private final HttpSessionCsrfTokenRepository csrf; private final InProcessProfileClient profiles;
  private final UserRepository users; private final IdentityApiMapper mapper;
  private final String gatewaySecret; private final String backendSecret;
  public InternalIdentityController(IdentityTokens t, SessionValidity s, HttpSessionCsrfTokenRepository c,
      InProcessProfileClient p, UserRepository u, IdentityApiMapper m,
      @Value("${identity.gateway-secret}") String g, @Value("${identity.backend-secret}") String b) {
    if (g.length()<32 || b.length()<32 || g.equals(b)) throw new IllegalArgumentException("Distinct service secrets of at least 32 characters required");
    tokens=t;sessions=s;csrf=c;profiles=p;users=u;mapper=m;gatewaySecret=g;backendSecret=b;
  }
  public record ExchangeRequest(String audience, String method, String csrfToken) {
    @Override public String toString() { return "ExchangeRequest[redacted]"; }
  }
  public record GrantRequest(String audience) {
    @Override public String toString() { return "GrantRequest[redacted]"; }
  }
  @PostMapping("/session-exchange")
  public ResponseEntity<?> exchange(@RequestBody ExchangeRequest body, HttpServletRequest request) {
    credential(request, "gateway", gatewaySecret);
    if (!"legacy-backend".equals(body.audience()) || !Set.of("GET","HEAD","OPTIONS","POST","PUT","PATCH","DELETE").contains(body.method()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    if (!Set.of("GET","HEAD","OPTIONS").contains(body.method())) {
      var expected=csrf.loadToken(request);
      var wrapped=new HttpServletRequestWrapper(request) {
        @Override public String getHeader(String name) { return "X-CSRF-TOKEN".equalsIgnoreCase(name) ? body.csrfToken() : super.getHeader(name); }
        @Override public String getParameter(String name) { return null; }
      };
      String supplied=expected==null ? null : new XorCsrfTokenRequestAttributeHandler().resolveCsrfTokenValue(wrapped, expected);
      if (expected==null || supplied==null || !MessageDigest.isEqual(expected.getToken().getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))
        return ResponseEntity.status(403).cacheControl(CacheControl.noStore()).body(Map.of("code","CSRF_INVALID",
            "message","Your security token has expired. Refresh and try again.","fieldErrors",Map.of()));
    }
    var auth=sessions.current(request);
    if(auth==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    var p=(CustomUserDetails)auth.getPrincipal();
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("assertion",tokens.issue(
        Integer.toString(p.getUserId()),body.audience(),"user",auth.getAuthorities().stream().map(a->a.getAuthority()).toList(),List.of())));
  }
  @PostMapping("/service-token")
  public ResponseEntity<?> grant(@RequestBody GrantRequest body, HttpServletRequest request) {
    credential(request,"legacy-backend",backendSecret);
    if (!"identity-service".equals(body.audience())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("accessToken",tokens.issue("legacy-backend",
        "identity-service","service",List.of(),List.of("public-profiles","checkout-profile","self-profile")),"expiresIn",60));
  }
  @GetMapping("/jwks") public Map<String,Object> keys() { return tokens.publicKeys(); }
  @GetMapping("/profiles") public ResponseEntity<?> batch(@RequestParam List<Integer> ids,HttpServletRequest r) {
    tokens.requireService(r.getHeader("Authorization"),"public-profiles");
    if(ids.size()>100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profiles.findAll(ids));
  }
  @GetMapping("/profiles/{profileId}") public ResponseEntity<?> byProfile(@PathVariable int profileId,HttpServletRequest r) {
    tokens.requireService(r.getHeader("Authorization"),"public-profiles");
    return profiles.findByProfileId(profileId).map(p->ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(p))
        .orElseGet(()->ResponseEntity.notFound().build());
  }
  @GetMapping("/users/{userId}/checkout-profile") public ResponseEntity<?> checkout(@PathVariable int userId,HttpServletRequest r) {
    tokens.requireService(r.getHeader("Authorization"),"checkout-profile");
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(profiles.checkout(userId));
  }
  @GetMapping("/users/{userId}/self-profile") public ResponseEntity<?> self(@PathVariable int userId,HttpServletRequest r) {
    tokens.requireService(r.getHeader("Authorization"),"self-profile");
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mapper.profile(users.findById(userId)
        .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND))));
  }
  private void credential(HttpServletRequest request,String client,String secret) {
    String expected="Basic "+Base64.getEncoder().encodeToString((client+":"+secret).getBytes(StandardCharsets.UTF_8));
    var values=Collections.list(request.getHeaders("Authorization"));
    String actual=values.size()==1 ? values.get(0) : null;
    if(actual==null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),actual.getBytes(StandardCharsets.UTF_8)))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  }
}
