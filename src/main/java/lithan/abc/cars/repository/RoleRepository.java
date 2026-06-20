package lithan.abc.cars.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.Role;
import lithan.abc.cars.entity.UserAccount;

public interface RoleRepository extends JpaRepository<Role, Integer> {

  boolean existsByUserAndRole(UserAccount user, String role);
}
