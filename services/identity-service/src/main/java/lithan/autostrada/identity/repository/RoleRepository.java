package lithan.autostrada.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.identity.entity.Role;
import lithan.autostrada.identity.entity.UserAccount;

public interface RoleRepository extends JpaRepository<Role, Integer> {

  boolean existsByUserAndRole(UserAccount user, String role);
}
