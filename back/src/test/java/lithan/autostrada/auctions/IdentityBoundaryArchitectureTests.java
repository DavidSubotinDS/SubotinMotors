package lithan.autostrada.auctions;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Runs in the required Backend suite and the production container build. */
class IdentityBoundaryArchitectureTests {
  // Existing identity-owned code retains its paths during preparation. Mixed
  // admin adapters own explicit account routes; their business operations delegate
  // to MarketplaceAdminService and use the separate business mapper.
  private static final Set<String> IDENTITY_OWNERS = Set.of(
      "entity/UserAccount.java", "entity/UserProfile.java", "entity/Role.java",
      "entity/ProfilePicture.java", "entity/PasswordResetToken.java",
      "repository/UserRepository.java", "repository/UserProfileRepository.java",
      "repository/RoleRepository.java", "repository/ProfilePictureRepository.java",
      "repository/PasswordResetTokenRepository.java", "dto/UserProfileForm.java",
      "service/UserService.java", "service/UserServiceImpl.java", "service/AdminService.java",
      "service/AdminServiceImpl.java", "service/PasswordResetService.java",
      "config/CustomUserDetails.java", "config/CustomUserDetailsService.java",
      "controller/LoginController.java", "controller/RegisterController.java",
      "controller/UserController.java", "controller/AdminController.java",
      "controller/api/AuthApiController.java", "controller/api/AdminApiController.java",
      "identity/IdentityApiMapper.java", "identity/SelfProfileService.java",
      "identity/InProcessProfileClient.java", "identity/SessionCurrentIdentity.java");

  @Test void onlyExplicitIdentityOwnersMayDependOnIdentityPersistenceOrLegacyIdentityServices() throws Exception {
    var root = Path.of("src/main/java/lithan/autostrada/auctions");
    var forbidden = Pattern.compile("\\b(UserAccount|UserProfile|ProfilePicture|PasswordResetToken|Role|"
        + "UserRepository|UserProfileRepository|RoleRepository|ProfilePictureRepository|PasswordResetTokenRepository|"
        + "UserService|AdminService|IdentityApiMapper)\\b|\\btb_(user|user_profile|role|profile_picture|password_reset_token)\\b");
    try (var files = Files.walk(root)) {
      for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String relative = root.relativize(file).toString().replace('\\', '/');
        if (!IDENTITY_OWNERS.contains(relative)) {
          assertThat(forbidden.matcher(Files.readString(file)).find())
              .as("Identity persistence dependency in %s; use CurrentIdentity/ProfileClient instead", relative).isFalse();
        }
      }
    }
  }

  @Test void identityAccountHasNoReverseBusinessCascadesAndPrincipalHasNoEntityState() throws Exception {
    assertThat(java.util.Arrays.stream(lithan.autostrada.auctions.entity.UserAccount.class.getDeclaredFields())
        .map(field -> field.getGenericType().getTypeName()))
        .noneMatch(type -> type.contains(".entity.Car"));
    assertThat(java.util.Arrays.stream(lithan.autostrada.auctions.config.CustomUserDetails.class.getDeclaredFields())
        .map(field -> field.getType().getName()))
        .noneMatch(type -> type.startsWith("lithan.autostrada.auctions.entity."));
    assertThat(lithan.autostrada.auctions.config.CustomUserDetails.class.getDeclaredField("userId").getModifiers())
        .matches(java.lang.reflect.Modifier::isFinal);
  }
}
