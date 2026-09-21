package lithan.autostrada.auctions.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.PaymentAccount;

public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, Integer> {
  Optional<PaymentAccount> findByUserId(int userId);

  Optional<PaymentAccount> findByProviderAccountId(String providerAccountId);
}
