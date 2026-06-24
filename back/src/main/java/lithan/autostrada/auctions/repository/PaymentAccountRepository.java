package lithan.autostrada.auctions.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.PaymentAccount;
import lithan.autostrada.auctions.entity.UserAccount;

public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, Integer> {
  Optional<PaymentAccount> findByUser(UserAccount user);

  Optional<PaymentAccount> findByProviderAccountId(String providerAccountId);
}
