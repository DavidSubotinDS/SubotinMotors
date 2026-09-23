package lithan.autostrada.auctions.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import lithan.autostrada.auctions.entity.CarPart;

public interface CarPartRepository
    extends JpaRepository<CarPart, Integer>, JpaSpecificationExecutor<CarPart> {

  Optional<CarPart> findBySkuIgnoreCase(String sku);

  boolean existsBySkuIgnoreCaseAndIdPartNot(String sku, int idPart);

  List<CarPart> findByActiveTrueAndStockQuantityGreaterThanOrderByNameAsc(int stockQuantity);

  @Query("select distinct part.category from CarPart part where part.active = true order by part.category")
  List<String> findActiveCategories();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select part from CarPart part where part.idPart in :ids order by part.idPart")
  List<CarPart> findAllByIdForUpdate(@Param("ids") List<Integer> ids);
}
