package lithan.abc.cars.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.StoreOrder;
import lithan.abc.cars.entity.UserAccount;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, Integer> {
  Page<StoreOrder> findByUser(UserAccount user, Pageable pageable);

  Optional<StoreOrder> findByIdOrderAndUser(int idOrder, UserAccount user);

  Optional<StoreOrder> findByCheckoutSessionId(String checkoutSessionId);
}
