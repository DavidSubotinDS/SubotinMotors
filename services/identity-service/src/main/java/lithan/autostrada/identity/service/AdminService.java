package lithan.autostrada.identity.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lithan.autostrada.identity.entity.UserAccount;
import lithan.autostrada.identity.entity.UserProfile;

public interface AdminService {

  void editUser(UserProfile profile);

  void markAsAdmin(int idUser);

  UserProfile getProfileById(int idCar);

  Page<UserAccount> listUser(Pageable pageable);

  Page<UserAccount> listAdmin(Pageable pageable);

}
