package lithan.autostrada.auctions.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.PasswordResetToken;
import lithan.autostrada.auctions.entity.UserAccount;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  void deleteByUser(UserAccount user);
}
