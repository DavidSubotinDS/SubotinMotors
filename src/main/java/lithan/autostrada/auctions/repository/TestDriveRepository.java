package lithan.autostrada.auctions.repository;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.Car;
import lithan.autostrada.auctions.entity.TestDrive;
import lithan.autostrada.auctions.entity.UserAccount;

public interface TestDriveRepository extends JpaRepository<TestDrive, Integer> {

  List<TestDrive> findByCarUserOrderByDateAsc(UserAccount user);

  List<TestDrive> findByUserOrderByDateAsc(UserAccount user);

  boolean existsByUserAndCarAndDate(UserAccount user, Car car, LocalDate date);

  boolean existsByUserAndCarAndDateAndIdTestDriveNot(
      UserAccount user, Car car, LocalDate date, int idTestDrive);
}
