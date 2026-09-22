package lithan.autostrada.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.identity.entity.PasswordResetToken;
import lithan.autostrada.identity.entity.UserAccount;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select token from PasswordResetToken token where token.tokenHash = :tokenHash")
  Optional<PasswordResetToken> findByTokenHashForUpdate(
      @org.springframework.data.repository.query.Param("tokenHash") String tokenHash);

  void deleteByUser(UserAccount user);
}
