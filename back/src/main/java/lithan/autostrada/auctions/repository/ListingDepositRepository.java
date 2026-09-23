package lithan.autostrada.auctions.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.ListingDeposit;

public interface ListingDepositRepository extends JpaRepository<ListingDeposit, Integer> {

  boolean existsByListingAndBuyerIdAndStatusIn(
      CarListing listing, int buyerId, Collection<String> statuses);

  Optional<ListingDeposit> findByCheckoutSessionId(String checkoutSessionId);

  Optional<ListingDeposit> findByCheckoutAttemptId(String checkoutAttemptId);

  Page<ListingDeposit> findByBuyerId(int buyerId, Pageable pageable);
}
