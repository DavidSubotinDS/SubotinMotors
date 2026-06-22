package lithan.abc.cars.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import lithan.abc.cars.entity.PasswordResetToken;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.repository.PasswordResetTokenRepository;
import lithan.abc.cars.repository.UserRepository;

@Service
public class PasswordResetService {

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;
  private final Clock clock;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Duration expiry;
  private final String baseUrl;

  public PasswordResetService(
      UserRepository userRepository,
      PasswordResetTokenRepository tokenRepository,
      PasswordEncoder passwordEncoder,
      EmailService emailService,
      Clock clock,
      @Value("${password-reset.expiry:PT30M}") Duration expiry,
      @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.emailService = emailService;
    this.clock = clock;
    this.expiry = expiry;
    this.baseUrl = baseUrl;
  }

  @Transactional
  public Optional<String> requestReset(String identifier) {
    if (identifier == null || identifier.isBlank()) {
      return Optional.empty();
    }

    Optional<UserAccount> userResult =
        userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(
            identifier.trim(), identifier.trim());
    if (userResult.isEmpty()) {
      return Optional.empty();
    }

    UserAccount user = userResult.get();
    tokenRepository.deleteByUser(user);

    String rawToken = generateToken();
    LocalDateTime now = LocalDateTime.now(clock);
    PasswordResetToken resetToken = new PasswordResetToken();
    resetToken.setUser(user);
    resetToken.setTokenHash(hash(rawToken));
    resetToken.setCreatedAt(now);
    resetToken.setExpiresAt(now.plus(expiry));
    tokenRepository.save(resetToken);

    String link = UriComponentsBuilder.fromUriString(baseUrl)
        .path("/reset-password")
        .queryParam("token", rawToken)
        .build()
        .toUriString();
    emailService.send(
        user.getEmail(),
        "Reset your Subotin Motors password",
        "Use this link to reset your password. It expires in "
            + expiry.toMinutes() + " minutes:\n" + link);
    return Optional.of(rawToken);
  }

  @Transactional(readOnly = true)
  public boolean isValid(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return false;
    }
    return tokenRepository.findByTokenHash(hash(rawToken))
        .filter(token -> token.getConsumedAt() == null)
        .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now(clock)))
        .isPresent();
  }

  @Transactional
  public boolean resetPassword(String rawToken, String newPassword) {
    if (rawToken == null || rawToken.isBlank()) {
      return false;
    }
    Optional<PasswordResetToken> tokenResult =
        tokenRepository.findByTokenHash(hash(rawToken));
    if (tokenResult.isEmpty()) {
      return false;
    }

    PasswordResetToken token = tokenResult.get();
    LocalDateTime now = LocalDateTime.now(clock);
    if (token.getConsumedAt() != null || !token.getExpiresAt().isAfter(now)) {
      return false;
    }

    UserAccount user = token.getUser();
    user.setPassword(passwordEncoder.encode(newPassword));
    token.setConsumedAt(now);
    userRepository.save(user);
    tokenRepository.save(token);
    return true;
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String rawToken) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }
}
