package lithan.autostrada.auctions.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.CarBidding;
import lithan.autostrada.auctions.entity.PaymentOrder;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Integer> {
  Optional<PaymentOrder> findByBid(CarBidding bid);

  Optional<PaymentOrder> findByCheckoutSessionId(String checkoutSessionId);

  List<PaymentOrder> findByBuyerIdOrderByCreatedAtDesc(int buyerId);

  List<PaymentOrder> findBySellerIdOrderByCreatedAtDesc(int sellerId);

  Page<PaymentOrder> findByBuyerId(int buyerId, Pageable pageable);

  Page<PaymentOrder> findBySellerId(int sellerId, Pageable pageable);
}
