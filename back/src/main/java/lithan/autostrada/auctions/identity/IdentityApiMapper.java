package lithan.autostrada.auctions.identity;

import java.util.List;
import org.springframework.stereotype.Component;
import lithan.autostrada.auctions.entity.*;
import lithan.autostrada.auctions.dto.api.ApiModels.ProfileResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.UserSummaryResponse;

/** Only self/admin identity endpoints may use this private mapper. */
@Component
public class IdentityApiMapper {
  public ProfileResponse profile(UserAccount user) {
    return profile(user.getProfile(), user);
  }

  public ProfileResponse profile(UserProfile profile, UserAccount user) {
    ProfilePicture picture = profile.getProfilePicture();
    return new ProfileResponse(
        profile.getIdProfile(),
        user == null ? null : user.getIdUser(),
        user == null ? null : user.getUsername(),
        user == null ? null : user.getEmail(),
        profile.getFirstName(),
        profile.getLastName(),
        profile.getPhoneNumber(),
        profile.getAddress(),
        profile.getStreetAddress(),
        profile.getCity(),
        profile.getPostalCode(),
        profile.getCountry(),
        profile.getAbout(),
        profile.hasCompleteShippingAddress(),
        profile.getFormattedShippingAddress(),
        profile.getDisplayLocation(),
        picture == null ? null : imageDataUrl(picture.getFileType(), picture.getImage()));
  }

  public UserSummaryResponse user(UserAccount user) {
    return new UserSummaryResponse(
        user.getIdUser(),
        user.getUsername(),
        user.getEmail(),
        user.getProfile() == null ? null : profile(user),
        user.getRoles() == null
            ? List.of()
            : user.getRoles().stream().map(Role::getRole).toList());
  }

  private String imageDataUrl(String type, String data) {
    return type == null || data == null ? null : "data:" + type + ";base64," + data;
  }
}
