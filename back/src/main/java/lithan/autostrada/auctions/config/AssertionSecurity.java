package lithan.autostrada.auctions.config;

import java.time.Duration;
import java.util.List;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;

/** Offline verification keys support overlapping kids without a startup dependency. */
@Configuration
public class AssertionSecurity {
  @Bean JwtDecoder identityDecoder(@Value("${identity.verification-jwks}") String json) throws Exception {
    var keys=JWKSet.parse(json);
    if(keys.getKeys().isEmpty() || keys.getKeys().stream().anyMatch(k->k.isPrivate() || k.getKeyID()==null))
      throw new IllegalArgumentException("Only public verification keys with kid may be supplied to backend");
    var processor=new DefaultJWTProcessor<com.nimbusds.jose.proc.SecurityContext>();
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256,new ImmutableJWKSet<>(keys)));
    var decoder=new NimbusJwtDecoder(processor);
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO),
        new JwtIssuerValidator("autostrada-identity"), jwt -> {
          try {
            var roles=jwt.getClaimAsStringList("roles");
            boolean valid=jwt.getAudience().equals(List.of("legacy-backend"))
                && "user".equals(jwt.getClaimAsString("tokenUse")) && Integer.parseInt(jwt.getSubject())>0
                && jwt.getId()!=null && jwt.getIssuedAt()!=null && jwt.getNotBefore()!=null && jwt.getExpiresAt()!=null
                && !jwt.getIssuedAt().isAfter(java.time.Instant.now())
                && Duration.between(jwt.getIssuedAt(),jwt.getExpiresAt()).getSeconds()<=60
                && roles!=null && roles.stream().allMatch(r->List.of("ROLE_USER","ROLE_ADMIN").contains(r));
            if(valid) return OAuth2TokenValidatorResult.success();
          } catch(RuntimeException ignored) { }
          return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        }));
    return decoder;
  }
  static JwtAuthenticationConverter converter() {
    var roles=new JwtGrantedAuthoritiesConverter();roles.setAuthoritiesClaimName("roles");roles.setAuthorityPrefix("");
    var converter=new JwtAuthenticationConverter();converter.setJwtGrantedAuthoritiesConverter(roles);return converter;
  }
}
