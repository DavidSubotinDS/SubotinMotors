package lithan.autostrada.identity.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import lithan.autostrada.identity.entity.UserAccount;

public interface UserRepository extends JpaRepository<UserAccount, Integer> {

  Optional<UserAccount> findByUsername(String username);

  Optional<UserAccount> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

  boolean existsByUsernameIgnoreCase(String username);

  boolean existsByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCaseAndIdUserNot(String email, int idUser);

  @Query("SELECT u FROM UserAccount u WHERE NOT EXISTS "
      + "(SELECT r FROM Role r WHERE r.user = u AND r.role = 'ROLE_ADMIN')")
  Page<UserAccount> findCustomers(Pageable pageable);

  @Query("SELECT u FROM UserAccount u WHERE EXISTS "
      + "(SELECT r FROM Role r WHERE r.user = u AND r.role = 'ROLE_ADMIN')")
  Page<UserAccount> findAdmins(Pageable pageable);

  @Query("SELECT u.password FROM UserAccount u WHERE u.idUser = :idUser")
  Optional<String> findPasswordHashByIdUser(int idUser);

  @Query("SELECT r.role FROM Role r WHERE r.user.idUser = :idUser ORDER BY r.role")
  List<String> findRoleNamesByIdUser(int idUser);
}
