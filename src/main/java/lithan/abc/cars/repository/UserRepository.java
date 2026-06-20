package lithan.abc.cars.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import lithan.abc.cars.entity.UserAccount;

public interface UserRepository extends JpaRepository<UserAccount, Integer> {

  Optional<UserAccount> findByUsername(String username);

  boolean existsByUsernameIgnoreCase(String username);

  @Query("SELECT u FROM UserAccount u WHERE NOT EXISTS "
      + "(SELECT r FROM Role r WHERE r.user = u AND r.role = 'ROLE_ADMIN')")
  Page<UserAccount> findCustomers(Pageable pageable);

  @Query("SELECT u FROM UserAccount u WHERE EXISTS "
      + "(SELECT r FROM Role r WHERE r.user = u AND r.role = 'ROLE_ADMIN')")
  Page<UserAccount> findAdmins(Pageable pageable);
}
