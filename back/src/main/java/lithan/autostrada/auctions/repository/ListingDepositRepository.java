package lithan.autostrada.auctions.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.ListingDeposit;
import lithan.autostrada.auctions.entity.UserAccount;

public interface ListingDepositRepository extends JpaRepository<ListingDeposit, Integer> {

  boolean existsByListingAndBuyerAndStatusIn(
      CarListing listing, UserAccount buyer, Collection<String> statuses);

  Optional<ListingDeposit> findByCheckoutSessionId(String checkoutSessionId);

  Page<ListingDeposit> findByBuyer(UserAccount buyer, Pageable pageable);
}
