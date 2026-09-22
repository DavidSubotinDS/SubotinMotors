package lithan.autostrada.identity.identity;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MultipartFile;
import lithan.autostrada.identity.dto.UserProfileForm;
import lithan.autostrada.identity.dto.api.ApiModels.ProfileRequest;
import lithan.autostrada.identity.dto.api.ApiModels.ProfileResponse;
import lithan.autostrada.identity.entity.UserAccount;
import lithan.autostrada.identity.service.UserService;

/** Self-profile operations owned by identity, including compatibility session state. */
@Service
public class SelfProfileService {
  private final UserService userService;
  private final IdentityApiMapper identityMapper;
  public SelfProfileService(UserService userService, IdentityApiMapper identityMapper) {
    this.userService = userService;
    this.identityMapper = identityMapper;
  }
  public ProfileResponse profile() {
    return identityMapper.profile(userService.getUserLogin());
  }

  public ProfileResponse updateProfile(
      ProfileRequest request,
      HttpSession session) {
    UserProfileForm form = new UserProfileForm();
    form.setIdProfile(userService.getUserLogin().getProfile().getIdProfile());
    form.setEmail(request.email());
    form.setFirstName(request.firstName());
    form.setLastName(request.lastName());
    form.setPhoneNumber(request.phoneNumber());
    form.setAddress(request.address());
    form.setStreetAddress(request.streetAddress());
    form.setCity(request.city());
    form.setPostalCode(request.postalCode());
    form.setCountry(request.country());
    form.setAbout(request.about());
    try {
      userService.editUserProfile(form);
    } catch (DataIntegrityViolationException exception) {
      throw new IllegalArgumentException("That email is already registered.");
    }
    UserAccount user = userService.getUserLogin();
    session.setAttribute("profileLog", user.getProfile());
    return identityMapper.profile(user);
  }

  public ProfileResponse updateProfilePicture(
      MultipartFile imageFile,
      HttpSession session) throws Exception {
    UserAccount user = userService.getUserLogin();
    userService.saveImage(imageFile, user.getProfile());
    session.setAttribute("profileLog", user.getProfile());
    return identityMapper.profile(userService.getUserLogin());
  }

}
