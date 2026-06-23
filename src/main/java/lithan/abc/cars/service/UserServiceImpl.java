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
import lithan.abc.cars.dto.UserProfileForm;
import lithan.abc.cars.repository.ProfilePictureRepository;
import lithan.abc.cars.error.ResourceNotFoundException;
import lithan.abc.cars.repository.UserProfileRepository;
import lithan.abc.cars.repository.UserRepository;
import lithan.abc.cars.validation.ImageUploadValidator;
import lithan.abc.cars.validation.ImageUploadValidator.ValidatedImage;

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
    if (userRepo.existsByEmailIgnoreCase(user.getEmail())) {
      throw new DataIntegrityViolationException("Email is already registered");
    }

    UserAccount saveUser = new UserAccount();
    Role role = new Role();

    saveUser.setUsername(user.getUsername());
    saveUser.setEmail(user.getEmail().trim().toLowerCase());
    saveUser.setPassword(passwordEncoder.encode(user.getPassword()));

    role.setRole("ROLE_USER");
    role.setUser(saveUser);

    normalizeAddress(profile);
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
    ValidatedImage image = ImageUploadValidator.validate(file);
    ProfilePicture picture = profile.getProfilePicture();
    if (picture == null) {
      picture = new ProfilePicture();
      picture.setProfile(profile);
    }

    picture.setFileName(image.fileName());
    picture.setFileType(image.contentType());
    picture.setImage(Base64.getEncoder().encodeToString(image.bytes()));
    profilePictureRepo.save(picture);
  }

  @Override
  @Transactional
  public void editUserProfile(UserProfileForm profile) {
    UserProfile currentProfile = getUserLogin().getProfile();
    if (currentProfile.getIdProfile() != profile.getIdProfile()) {
      throw new org.springframework.security.access.AccessDeniedException("Cannot edit another profile");
    }
    UserAccount user = currentProfile.getUser();
    String normalizedEmail = profile.getEmail().trim().toLowerCase();
    if (userRepo.existsByEmailIgnoreCaseAndIdUserNot(normalizedEmail, user.getIdUser())) {
      throw new DataIntegrityViolationException("Email is already registered");
    }
    UserProfile editedProfile = userProfileRepo.findById(profile.getIdProfile())
        .orElseThrow(ResourceNotFoundException::new);

    user.setEmail(normalizedEmail);
    editedProfile.setFirstName(profile.getFirstName());
    editedProfile.setLastName(profile.getLastName());
    editedProfile.setPhoneNumber(profile.getPhoneNumber());
    editedProfile.setAddress(profile.getAddress());
    editedProfile.setStreetAddress(trimToNull(profile.getStreetAddress()));
    editedProfile.setCity(trimToNull(profile.getCity()));
    editedProfile.setPostalCode(trimToNull(profile.getPostalCode()));
    editedProfile.setCountry(trimToNull(profile.getCountry()));
    editedProfile.setAbout(profile.getAbout());

    userRepo.save(user);
    userProfileRepo.save(editedProfile);
  }

  @Override
  public UserProfile getProfile(int idProfile) {
    return userProfileRepo.findById(idProfile).orElseThrow(ResourceNotFoundException::new);
  }

  private void normalizeAddress(UserProfile profile) {
    profile.setAddress(trimToNull(profile.getAddress()));
    profile.setStreetAddress(trimToNull(profile.getStreetAddress()));
    profile.setCity(trimToNull(profile.getCity()));
    profile.setPostalCode(trimToNull(profile.getPostalCode()));
    profile.setCountry(trimToNull(profile.getCountry()));
  }

  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

}
