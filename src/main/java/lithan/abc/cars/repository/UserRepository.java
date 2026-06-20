package lithan.abc.cars.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.abc.cars.entity.UserAccount;

public interface UserRepository extends JpaRepository<UserAccount, Integer> {

  Optional<UserAccount> findByUsername(String username);

  boolean existsByUsernameIgnoreCase(String username);
}
