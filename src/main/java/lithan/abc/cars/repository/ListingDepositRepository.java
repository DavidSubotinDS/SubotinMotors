package lithan.abc.cars.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.CarListing;
import lithan.abc.cars.entity.ListingDeposit;
import lithan.abc.cars.entity.UserAccount;

public interface ListingDepositRepository extends JpaRepository<ListingDeposit, Integer> {

  boolean existsByListingAndBuyerAndStatusIn(
      CarListing listing, UserAccount buyer, Collection<String> statuses);

  Optional<ListingDeposit> findByCheckoutSessionId(String checkoutSessionId);

  Page<ListingDeposit> findByBuyer(UserAccount buyer, Pageable pageable);
}
