package lithan.autostrada.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class AuthorizationTests {
  static final RSAKey KEY;
  static {try{KEY=new com.nimbusds.jose.jwk.gen.RSAKeyGenerator(2048).keyID("notification-test").generate();}catch(Exception e){throw new ExceptionInInitializerError(e);}}
  @DynamicPropertySource static void properties(DynamicPropertyRegistry p) {
    p.add("spring.datasource.url",()->"jdbc:h2:mem:notification_security;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    p.add("spring.datasource.username",()->"sa");p.add("spring.datasource.password",()->"");
    p.add("identity.verification-jwks",()->new JWKSet(KEY.toPublicJWK()).toString());
    p.add("notification.delivery-key",()->Base64.getEncoder().encodeToString(new byte[32]));
    p.add("notification.broker.enabled",()->"false");p.add("notification.delivery.enabled",()->"false");
  }
  @Autowired MockMvc mvc;
  String token(String issuer,String audience,String use,String subject,Instant start,List<String> roles,List<String> scopes) {
    var claims=JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience)).subject(subject).issuedAt(start).notBefore(start)
        .expiresAt(start.plusSeconds(60)).id(UUID.randomUUID().toString()).claim("tokenUse",use).claim("roles",roles).claim("scopes",scopes).build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(KEY))).encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId(KEY.getKeyID()).build(),claims)).getTokenValue();
  }
  @Test void rejectsWrongIssuerAudienceUseExpiredAndFutureAssertions() throws Exception {
    Instant now=Instant.now();
    for(String value:List.of(token("wrong","notification-service","user","2",now,List.of("ROLE_USER"),List.of()),
        token("autostrada-identity","legacy-backend","user","2",now,List.of("ROLE_USER"),List.of()),
        token("autostrada-identity","notification-service","service","2",now,List.of("ROLE_USER"),List.of()),
        token("autostrada-identity","notification-service","user","2",now.minusSeconds(120),List.of("ROLE_USER"),List.of()),
        token("autostrada-identity","notification-service","user","2",now.plusSeconds(60),List.of("ROLE_USER"),List.of())))
      mvc.perform(get("/api/user/notifications").header("Authorization","Bearer "+value)).andExpect(status().isUnauthorized());
  }
  @Test void serviceCountGrantCannotReadInboxOrMutateReadState() throws Exception {
    String value=token("autostrada-identity","notification-service","service","legacy-backend",Instant.now(),List.of(),List.of("notification-count"));
    mvc.perform(get("/internal/v1/users/2/unread-count").header("Authorization","Bearer "+value)).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
    mvc.perform(get("/api/user/notifications").header("Authorization","Bearer "+value)).andExpect(status().isForbidden());
    mvc.perform(post("/api/user/notifications/read-all").header("Authorization","Bearer "+value)).andExpect(status().isForbidden());
  }
  @Test void userRoleRequiredAndCookiesAndForgedIdsCannotAuthenticate() throws Exception {
    mvc.perform(get("/api/user/notifications").header("X-User-ID","2").cookie(new jakarta.servlet.http.Cookie("AUTOSTRADA_SESSION","forged"))).andExpect(status().isUnauthorized());
    String user=token("autostrada-identity","notification-service","user","2",Instant.now(),List.of("ROLE_USER"),List.of());
    mvc.perform(get("/api/user/notifications").header("Authorization","Bearer "+user)).andExpect(status().isOk());
    mvc.perform(get("/internal/v1/users/3/unread-count").header("Authorization","Bearer "+user)).andExpect(status().isForbidden());
    String admin=token("autostrada-identity","notification-service","user","2",Instant.now(),List.of("ROLE_ADMIN"),List.of());
    mvc.perform(get("/api/user/notifications").header("Authorization","Bearer "+admin)).andExpect(status().isForbidden());
  }
}
