package lithan.autostrada.identity.controller.api;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import lithan.autostrada.identity.dto.UserProfileForm;
import lithan.autostrada.identity.dto.api.ApiModels.*;
import lithan.autostrada.identity.dto.api.PageResponse;
import lithan.autostrada.identity.entity.UserProfile;
import lithan.autostrada.identity.service.AdminService;
import lithan.autostrada.identity.identity.IdentityApiMapper;
@RestController @RequestMapping("/api/admin")
public class AdminApiController {
 private final AdminService adminService; private final IdentityApiMapper identityMapper;
 public AdminApiController(AdminService a, IdentityApiMapper m) { adminService=a; identityMapper=m; }
  @GetMapping("/dashboard")
  public AdminDashboardResponse dashboard(
      @RequestParam(defaultValue = "0") int userPage,
      @RequestParam(defaultValue = "idUser") String userSort,
      @RequestParam(defaultValue = "asc") String userDirection,
      @RequestParam(defaultValue = "0") int adminPage,
      @RequestParam(defaultValue = "idUser") String adminSort,
      @RequestParam(defaultValue = "asc") String adminDirection) {
    var users = adminService.listUser(PageRequest.of(
        Math.max(userPage, 0),
        5,
        Sort.by(sortDirection(userDirection), userSortProperty(userSort))));
    var admins = adminService.listAdmin(PageRequest.of(
        Math.max(adminPage, 0),
        5,
        Sort.by(sortDirection(adminDirection), userSortProperty(adminSort))));
    return new AdminDashboardResponse(
        PageResponse.from(users.map(identityMapper::user)),
        PageResponse.from(admins.map(identityMapper::user)));
  }

  @GetMapping("/users/{idProfile}")
  public ProfileResponse userProfile(@PathVariable int idProfile) {
    return identityMapper.profile(adminService.getProfileById(idProfile), null);
  }

  @PutMapping("/users/{idProfile}")
  public ProfileResponse updateUserProfile(
      @PathVariable int idProfile,
      @RequestBody ProfileRequest request) {
    UserProfileForm form = new UserProfileForm();
    form.setIdProfile(idProfile);
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
    UserProfile profile = new UserProfile();
    profile.setIdProfile(form.getIdProfile());
    profile.setFirstName(form.getFirstName());
    profile.setLastName(form.getLastName());
    profile.setPhoneNumber(form.getPhoneNumber());
    profile.setAddress(form.getAddress());
    profile.setStreetAddress(form.getStreetAddress());
    profile.setCity(form.getCity());
    profile.setPostalCode(form.getPostalCode());
    profile.setCountry(form.getCountry());
    profile.setAbout(form.getAbout());
    adminService.editUser(profile);
    return identityMapper.profile(adminService.getProfileById(idProfile), null);
  }

  @PostMapping("/users/{idUser}/mark-admin")
  public ApiMessageResponse markAdmin(@PathVariable int idUser) {
    adminService.markAsAdmin(idUser);
    return new ApiMessageResponse("User promoted to admin.", null);
  }

  private Sort.Direction sortDirection(String direction) {
    return "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
  }

  private String userSortProperty(String sort) {
    return switch (sort) {
      case "idUser", "username", "email", "profile.firstName", "profile.lastName" -> sort;
      default -> "idUser";
    };
  }

}
