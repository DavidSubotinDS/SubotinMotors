package lithan.autostrada.identity.config;

import java.time.Instant;
import java.util.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

/** Identity is the only holder of the signing key. No token or key is logged. */
@Component
public class IdentityTokens {
  private final RSAKey key;
  private final JwtEncoder encoder;
  private final JwtDecoder decoder;
  private final String issuer;
  public IdentityTokens(@Value("${identity.signing-jwk}") String json,
      @Value("${identity.issuer:autostrada-identity}") String issuer) throws Exception {
    key = RSAKey.parse(json);
    if (!key.isPrivate() || key.getKeyID() == null || key.size() < 2048)
      throw new IllegalArgumentException("A private RSA signing key with kid and at least 2048 bits is required");
    this.issuer = issuer;
    encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    var verifier = NimbusJwtDecoder.withPublicKey(key.toRSAPublicKey()).signatureAlgorithm(SignatureAlgorithm.RS256).build();
    verifier.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
        new JwtTimestampValidator(java.time.Duration.ZERO), new JwtIssuerValidator(issuer), jwt -> {
          boolean valid = jwt.getAudience().equals(List.of("identity-service"))
              && "service".equals(jwt.getClaimAsString("tokenUse"))
              && "legacy-backend".equals(jwt.getSubject()) && jwt.getExpiresAt() != null
              && jwt.getNotBefore() != null && jwt.getIssuedAt() != null && jwt.getId() != null;
          return valid ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
              : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                  new org.springframework.security.oauth2.core.OAuth2Error("invalid_token"));
        }));
    decoder = verifier;
  }
  public String issue(String subject, String audience, String use, List<String> roles, List<String> scopes) {
    var now = Instant.now();
    var claims = JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience)).subject(subject)
        .issuedAt(now).notBefore(now).expiresAt(now.plusSeconds(60)).id(UUID.randomUUID().toString())
        .claim("tokenUse", use).claim("roles", roles).claim("scopes", scopes).build();
    return encoder.encode(JwtEncoderParameters.from(
        JwsHeader.with(SignatureAlgorithm.RS256).keyId(key.getKeyID()).build(), claims)).getTokenValue();
  }
  public void requireService(String authorization, String scope) {
    try {
      if (authorization == null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException();
      var token = decoder.decode(authorization.substring(7));
      if (!token.getClaimAsStringList("scopes").contains(scope)) throw new IllegalArgumentException();
    } catch (RuntimeException ex) {
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }
  }
  public Map<String,Object> publicKeys() { return new JWKSet(key.toPublicJWK()).toJSONObject(); }
}
