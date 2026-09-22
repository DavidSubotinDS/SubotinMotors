package fixtures.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fixtures.identity.entity.PasswordResetToken;
import fixtures.identity.entity.UserAccount;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  void deleteByUser(UserAccount user);
}
