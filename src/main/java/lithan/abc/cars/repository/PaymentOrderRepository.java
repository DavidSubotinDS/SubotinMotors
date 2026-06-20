package lithan.abc.cars.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.CarBidding;
import lithan.abc.cars.entity.PaymentOrder;
import lithan.abc.cars.entity.UserAccount;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Integer> {
  Optional<PaymentOrder> findByBid(CarBidding bid);

  Optional<PaymentOrder> findByCheckoutSessionId(String checkoutSessionId);

  List<PaymentOrder> findByBuyerOrderByCreatedAtDesc(UserAccount buyer);

  List<PaymentOrder> findBySellerOrderByCreatedAtDesc(UserAccount seller);

  Page<PaymentOrder> findByBuyer(UserAccount buyer, Pageable pageable);

  Page<PaymentOrder> findBySeller(UserAccount seller, Pageable pageable);
}
