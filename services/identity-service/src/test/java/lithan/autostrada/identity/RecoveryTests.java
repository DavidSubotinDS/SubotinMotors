package lithan.autostrada.identity;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import lithan.autostrada.identity.entity.*;
import lithan.autostrada.identity.repository.*;
import lithan.autostrada.identity.service.PasswordResetService;
@SpringBootTest @Transactional
class RecoveryTests extends IdentityTestBase {
 @Autowired PasswordResetService passwordResetService;
 @Autowired PasswordResetTokenRepository tokenRepository;
 @Autowired UserRepository userRepository;
 @Autowired PasswordEncoder passwordEncoder;

  @Test
  void passwordResetTokenIsHashedConsumedAndChangesPassword() {
    UserAccount user = userRepository.findByUsername("user123").orElseThrow();
    String oldPassword = user.getPassword();

    String rawToken = passwordResetService.requestReset("user123").orElseThrow();
    PasswordResetToken stored = tokenRepository.findAll().stream()
        .filter(token -> token.getUser().getIdUser() == user.getIdUser())
        .findFirst()
        .orElseThrow();

    assertNotEquals(rawToken, stored.getTokenHash());
    assertTrue(passwordResetService.isValid(rawToken));
    assertTrue(passwordResetService.resetPassword(rawToken, "newSecret123"));
    assertFalse(passwordResetService.isValid(rawToken));
    assertFalse(passwordResetService.resetPassword(rawToken, "anotherSecret123"));
    assertTrue(passwordEncoder.matches(
        "newSecret123",
        userRepository.findById(user.getIdUser()).orElseThrow().getPassword()));
    assertFalse(passwordEncoder.matches(
        "newSecret123", oldPassword));
  }
  @Test
  void expiredAndInvalidResetTokensAreRejected() {
    UserAccount user = userRepository.findByUsername("user123").orElseThrow();
    String rawToken = passwordResetService.requestReset(user.getEmail()).orElseThrow();
    PasswordResetToken stored = tokenRepository.findAll().stream()
        .filter(token -> token.getUser().getIdUser() == user.getIdUser())
        .findFirst()
        .orElseThrow();
    stored.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    tokenRepository.saveAndFlush(stored);

    assertFalse(passwordResetService.isValid(rawToken));
    assertFalse(passwordResetService.resetPassword(rawToken, "newSecret123"));
    assertFalse(passwordResetService.isValid("not-a-real-token"));
  }
}
