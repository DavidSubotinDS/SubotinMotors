package lithan.abc.cars.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import lithan.abc.cars.entity.CarListing;
import lithan.abc.cars.entity.CarListingStatus;
import lithan.abc.cars.entity.UserAccount;

public interface CarListingRepository extends JpaRepository<CarListing, Integer> {

  @Query("SELECT l FROM CarListing l WHERE l.status IN :statuses "
      + "AND (:keyword IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
      + "OR LOWER(l.make) LIKE LOWER(CONCAT('%', :keyword, '%')) "
      + "OR LOWER(l.model) LIKE LOWER(CONCAT('%', :keyword, '%')) "
      + "OR l.year LIKE CONCAT('%', :keyword, '%'))")
  Page<CarListing> browse(
      @Param("statuses") Collection<CarListingStatus> statuses,
      @Param("keyword") String keyword,
      Pageable pageable);

  List<CarListing> findBySellerOrderByCreatedAtDesc(UserAccount seller);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT l FROM CarListing l WHERE l.idListing = :id")
  Optional<CarListing> findByIdForUpdate(@Param("id") int id);
}
