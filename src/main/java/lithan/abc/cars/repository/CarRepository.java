package lithan.abc.cars.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.UserAccount;

public interface CarRepository extends JpaRepository<Car, Integer> {

        List<Car> findByStatusNot(String status);

        List<Car> findByUser(UserAccount user);

        @Query("SELECT c FROM Car c WHERE c.status <> 'DEACTIVE' AND "
                        + "(LOWER(c.make) LIKE LOWER(CONCAT('%', :keyword, '%')) "
                        + "OR LOWER(c.model) LIKE LOWER(CONCAT('%', :keyword, '%')) "
                        + "OR c.year LIKE CONCAT('%', :keyword, '%'))")
        List<Car> searchCar(@Param("keyword") String keyword);

        @Query("SELECT c FROM Car c WHERE c.status <> 'DEACTIVE' AND c.price BETWEEN :low AND :high")
        List<Car> searchCarByPriceRange(@Param("low") int low, @Param("high") int high);

        @Query("SELECT c FROM Car c WHERE c.status <> 'DEACTIVE' AND c.price BETWEEN :low AND :high AND "
                        + "(LOWER(c.make) LIKE LOWER(CONCAT('%', :keyword, '%')) "
                        + "OR LOWER(c.model) LIKE LOWER(CONCAT('%', :keyword, '%')) "
                        + "OR c.year LIKE CONCAT('%', :keyword, '%'))")
        List<Car> searchCarByKeywordAndPriceRange(@Param("keyword") String keyword, @Param("low") int low,
                        @Param("high") int high);

        @Query("SELECT c FROM Car c WHERE c.status = 'ACTIVE' ORDER BY c.idCar DESC")
        List<Car> featuredCars(Pageable pageable);
}
