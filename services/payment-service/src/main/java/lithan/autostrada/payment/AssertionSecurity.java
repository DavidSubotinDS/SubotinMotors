package lithan.autostrada.payment;

import java.time.Duration;
import java.util.*;
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

@Configuration
class AssertionSecurity {
  @Bean JwtDecoder identityDecoder(@Value("${identity.verification-jwks}") String json) throws Exception {
    var keys=JWKSet.parse(json);
    if(keys.getKeys().isEmpty() || keys.getKeys().stream().anyMatch(k->k.isPrivate() || k.getKeyID()==null))
      throw new IllegalArgumentException("Only public identity keys with kid may verify payment assertions");
    var processor=new DefaultJWTProcessor<com.nimbusds.jose.proc.SecurityContext>();
    processor.setJWSKeySelector(new JWSVerificationKeySelector<>(JWSAlgorithm.RS256,new ImmutableJWKSet<>(keys)));
    var decoder=new NimbusJwtDecoder(processor);
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(Duration.ZERO),
        new JwtIssuerValidator("autostrada-identity"),jwt->{
          try {
            var roles=Optional.ofNullable(jwt.getClaimAsStringList("roles")).orElse(List.of());
            var scopes=Optional.ofNullable(jwt.getClaimAsStringList("scopes")).orElse(List.of());
            boolean service="service".equals(jwt.getClaimAsString("tokenUse"))
                && "legacy-backend".equals(jwt.getSubject()) && roles.isEmpty()
                && new HashSet<>(scopes).equals(Set.of("create-store-payment","create-deposit-payment","payment-lookup","payment-expire"));
            boolean user="user".equals(jwt.getClaimAsString("tokenUse"))
                && Integer.parseInt(jwt.getSubject())>0 && scopes.isEmpty()
                && roles.stream().allMatch(r->Set.of("ROLE_USER","ROLE_ADMIN").contains(r));
            boolean valid=jwt.getAudience().equals(List.of("payment-service")) && (service||user)
                && jwt.getId()!=null && jwt.getIssuedAt()!=null && jwt.getNotBefore()!=null && jwt.getExpiresAt()!=null
                && !jwt.getIssuedAt().isAfter(java.time.Instant.now())
                && Duration.between(jwt.getIssuedAt(),jwt.getExpiresAt()).getSeconds()<=60;
            if(valid)return OAuth2TokenValidatorResult.success();
          }catch(RuntimeException ignored) { }
          return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        }));
    return decoder;
  }
  static JwtAuthenticationConverter converter() {
    var converter=new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt->{
      java.util.Collection<org.springframework.security.core.GrantedAuthority> authorities;
      if("service".equals(jwt.getClaimAsString("tokenUse"))) authorities=jwt.getClaimAsStringList("scopes").stream()
          .<org.springframework.security.core.GrantedAuthority>map(s->new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_"+s)).toList();
      else authorities=jwt.getClaimAsStringList("roles").stream()
          .<org.springframework.security.core.GrantedAuthority>map(org.springframework.security.core.authority.SimpleGrantedAuthority::new).toList();
      return authorities;
    });
    return converter;
  }
}
