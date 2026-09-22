package fixtures.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import fixtures.identity.entity.Role;
import fixtures.identity.entity.UserAccount;

public interface RoleRepository extends JpaRepository<Role, Integer> {

  boolean existsByUserAndRole(UserAccount user, String role);
}
