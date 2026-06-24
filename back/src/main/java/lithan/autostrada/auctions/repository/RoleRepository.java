package lithan.autostrada.auctions.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.Role;
import lithan.autostrada.auctions.entity.UserAccount;

public interface RoleRepository extends JpaRepository<Role, Integer> {

  boolean existsByUserAndRole(UserAccount user, String role);
}
