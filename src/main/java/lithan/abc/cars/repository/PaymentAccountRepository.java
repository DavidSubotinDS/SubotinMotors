package lithan.abc.cars.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.PaymentAccount;
import lithan.abc.cars.entity.UserAccount;

public interface PaymentAccountRepository extends JpaRepository<PaymentAccount, Integer> {
  Optional<PaymentAccount> findByUser(UserAccount user);

  Optional<PaymentAccount> findByProviderAccountId(String providerAccountId);
}
