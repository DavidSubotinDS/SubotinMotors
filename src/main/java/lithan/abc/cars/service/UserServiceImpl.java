package lithan.abc.cars.service;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lithan.abc.cars.entity.ProfilePicture;
import lithan.abc.cars.entity.Role;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.entity.UserProfile;
import lithan.abc.cars.repository.ProfilePictureRepository;
import lithan.abc.cars.error.ResourceNotFoundException;
import lithan.abc.cars.repository.UserProfileRepository;
import lithan.abc.cars.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

  @Autowired
  private UserRepository userRepo;

  @Autowired
  private UserProfileRepository userProfileRepo;

  @Autowired
  private ProfilePictureRepository profilePictureRepo;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void saveUser(UserAccount user, UserProfile profile) {
    if (userRepo.existsByUsernameIgnoreCase(user.getUsername())) {
      throw new DataIntegrityViolationException("Username is already registered");
    }

    UserAccount saveUser = new UserAccount();
    Role role = new Role();

    saveUser.setUsername(user.getUsername());
    saveUser.setPassword(passwordEncoder.encode(user.getPassword()));

    role.setRole("ROLE_USER");
    role.setUser(saveUser);

    profile.setUser(saveUser);

    saveUser.setProfile(profile);
    saveUser.setRoles(java.util.Collections.singletonList(role));

    userRepo.save(saveUser);
  }

  @Override
  public UserAccount findByUsername(String username) {
    return userRepo.findByUsername(username).orElse(null);
  }

  @Override
  public UserAccount getUserLogin() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    String username = authentication.getName();

    return userRepo.findByUsername(username).orElseThrow(ResourceNotFoundException::new);
  }

  @Override
  @Transactional
  public void saveImage(MultipartFile file, UserProfile profile) throws Exception {
      if (profile.getProfilePicture() == null) {
        // Set Profile Picture if no profile picture
        ProfilePicture picture = new ProfilePicture();

        picture.setFileName(file.getOriginalFilename());
        picture.setFileType(file.getContentType());
        picture.setImage(Base64.getEncoder().encodeToString(file.getBytes()));
        picture.setProfile(profile);

        profilePictureRepo.save(picture);

      } else {
        // Edit Profile Picture if profile picture exist
        ProfilePicture picture = profile.getProfilePicture();

        picture.setFileName(file.getOriginalFilename());
        picture.setFileType(file.getContentType());
        picture.setImage(Base64.getEncoder().encodeToString(file.getBytes()));
        picture.setProfile(profile);

        profilePictureRepo.save(picture);
      }
  }

  @Override
  @Transactional
  public void editUserProfile(UserProfile profile) {
    UserProfile currentProfile = getUserLogin().getProfile();
    if (currentProfile.getIdProfile() != profile.getIdProfile()) {
      throw new org.springframework.security.access.AccessDeniedException("Cannot edit another profile");
    }
    UserProfile editedProfile = userProfileRepo.findById(profile.getIdProfile())
        .orElseThrow(ResourceNotFoundException::new);

    editedProfile.setFirstName(profile.getFirstName());
    editedProfile.setLastName(profile.getLastName());
    editedProfile.setPhoneNumber(profile.getPhoneNumber());
    editedProfile.setAddress(profile.getAddress());
    editedProfile.setAbout(profile.getAbout());

    userProfileRepo.save(editedProfile);
  }

  @Override
  public UserProfile getProfile(int idProfile) {
    return userProfileRepo.findById(idProfile).orElseThrow(ResourceNotFoundException::new);
  }

}
