package lithan.abc.cars.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.TestDrive;
import lithan.abc.cars.entity.UserAccount;

public interface TestDriveRepository extends JpaRepository<TestDrive, Integer> {

  List<TestDrive> findByCarUser(UserAccount user);
}
