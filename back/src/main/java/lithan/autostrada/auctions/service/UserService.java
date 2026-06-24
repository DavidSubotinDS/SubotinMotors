package lithan.autostrada.auctions.service;

import org.springframework.web.multipart.MultipartFile;

import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.entity.UserProfile;
import lithan.autostrada.auctions.dto.UserProfileForm;

public interface UserService {

  void saveUser(UserAccount user, UserProfile profile);

  UserAccount findByUsername(String username);

  UserAccount getUserLogin();

  void saveImage(MultipartFile file, UserProfile profile) throws Exception;

  void editUserProfile(UserProfileForm profile);

  UserProfile getProfile(int idProfile);
}
