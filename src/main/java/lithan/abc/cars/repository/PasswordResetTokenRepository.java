package lithan.abc.cars.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.PasswordResetToken;
import lithan.abc.cars.entity.UserAccount;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  void deleteByUser(UserAccount user);
}
