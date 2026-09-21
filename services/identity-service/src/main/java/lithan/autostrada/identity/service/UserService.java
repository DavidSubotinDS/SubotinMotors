package lithan.autostrada.identity.service;

import org.springframework.web.multipart.MultipartFile;

import lithan.autostrada.identity.entity.UserAccount;
import lithan.autostrada.identity.entity.UserProfile;
import lithan.autostrada.identity.dto.UserProfileForm;

public interface UserService {

  void saveUser(UserAccount user, UserProfile profile);

  UserAccount findByUsername(String username);

  UserAccount getUserLogin();

  void saveImage(MultipartFile file, UserProfile profile) throws Exception;

  void editUserProfile(UserProfileForm profile);

  UserProfile getProfile(int idProfile);
}
