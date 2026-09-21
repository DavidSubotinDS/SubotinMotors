package lithan.autostrada.auctions.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.StoreOrder;

public interface StoreOrderRepository extends JpaRepository<StoreOrder, Integer> {
  Page<StoreOrder> findByUserId(int userId, Pageable pageable);

  Optional<StoreOrder> findByIdOrderAndUserId(int idOrder, int userId);

  Optional<StoreOrder> findByCheckoutSessionId(String checkoutSessionId);
}
