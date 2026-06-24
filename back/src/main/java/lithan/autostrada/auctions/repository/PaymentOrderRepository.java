package lithan.autostrada.auctions.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.CarBidding;
import lithan.autostrada.auctions.entity.PaymentOrder;
import lithan.autostrada.auctions.entity.UserAccount;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Integer> {
  Optional<PaymentOrder> findByBid(CarBidding bid);

  Optional<PaymentOrder> findByCheckoutSessionId(String checkoutSessionId);

  List<PaymentOrder> findByBuyerOrderByCreatedAtDesc(UserAccount buyer);

  List<PaymentOrder> findBySellerOrderByCreatedAtDesc(UserAccount seller);

  Page<PaymentOrder> findByBuyer(UserAccount buyer, Pageable pageable);

  Page<PaymentOrder> findBySeller(UserAccount seller, Pageable pageable);
}
