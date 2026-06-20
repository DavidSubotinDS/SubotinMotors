package lithan.abc.cars.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.UserAccount;

public interface CarRepository extends JpaRepository<Car, Integer> {

        List<Car> findByStatusNot(String status);

        List<Car> findByUser(UserAccount user);

        @Query("SELECT c FROM Car c WHERE c.status = 'ACTIVE' "
                        + "AND (:keyword IS NULL OR LOWER(c.make) LIKE LOWER(CONCAT('%', :keyword, '%')) "
                        + "OR LOWER(c.model) LIKE LOWER(CONCAT('%', :keyword, '%')) "
                        + "OR c.year LIKE CONCAT('%', :keyword, '%')) "
                        + "AND (:low IS NULL OR c.price >= :low) "
                        + "AND (:high IS NULL OR c.price <= :high)")
        Page<Car> findCatalogCars(
                        @Param("keyword") String keyword,
                        @Param("low") Integer low,
                        @Param("high") Integer high,
                        Pageable pageable);

        @Query("SELECT c FROM Car c WHERE c.status = 'ACTIVE' ORDER BY c.idCar DESC")
        List<Car> featuredCars(Pageable pageable);
}
