package lithan.autostrada.auctions.repository;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.TestDrive;

public interface TestDriveRepository extends JpaRepository<TestDrive, Integer> {

  List<TestDrive> findByCarUserIdOrderByDateAsc(int userId);

  List<TestDrive> findByUserIdOrderByDateAsc(int userId);

  boolean existsByUserIdAndCarAndDate(int userId, Car car, LocalDate date);

  boolean existsByUserIdAndCarAndDateAndIdTestDriveNot(
      int userId, Car car, LocalDate date, int idTestDrive);
}
