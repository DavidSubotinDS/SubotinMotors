package lithan.autostrada.auctions.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.StoreOrder;
import lithan.autostrada.auctions.entity.UserAccount;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, Integer> {
  Page<StoreOrder> findByUser(UserAccount user, Pageable pageable);

  Optional<StoreOrder> findByIdOrderAndUser(int idOrder, UserAccount user);

  Optional<StoreOrder> findByCheckoutSessionId(String checkoutSessionId);
}
