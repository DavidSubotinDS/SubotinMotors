package lithan.abc.cars.repository;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.Car;
import lithan.abc.cars.entity.TestDrive;
import lithan.abc.cars.entity.UserAccount;

public interface TestDriveRepository extends JpaRepository<TestDrive, Integer> {

  List<TestDrive> findByCarUserOrderByDateAsc(UserAccount user);

  List<TestDrive> findByUserOrderByDateAsc(UserAccount user);

  boolean existsByUserAndCarAndDate(UserAccount user, Car car, LocalDate date);

  boolean existsByUserAndCarAndDateAndIdTestDriveNot(
      UserAccount user, Car car, LocalDate date, int idTestDrive);
}
