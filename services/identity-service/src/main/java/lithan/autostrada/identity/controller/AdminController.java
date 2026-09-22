package lithan.autostrada.identity.controller;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import lithan.autostrada.identity.entity.*;
import lithan.autostrada.identity.service.*;
@Controller @RequestMapping("/admin")
public class AdminController {
 @Autowired private AdminService adminService;
 @Autowired private UserService userService;
  @GetMapping("")
  public String admin() {

    return "redirect:/admin/dashboard";
  }

  @GetMapping("/dashboard")
  public String dashboard(
      @RequestParam(defaultValue = "0") int userPage,
      @RequestParam(defaultValue = "idUser") String userSort,
      @RequestParam(defaultValue = "asc") String userDirection,
      @RequestParam(defaultValue = "0") int adminPage,
      @RequestParam(defaultValue = "idUser") String adminSort,
      @RequestParam(defaultValue = "asc") String adminDirection,
      Model model,
      HttpSession session) {
    String safeUserSort = userSortProperty(userSort);
    String safeAdminSort = userSortProperty(adminSort);
    Sort.Direction safeUserDirection = sortDirection(userDirection);
    Sort.Direction safeAdminDirection = sortDirection(adminDirection);
    Page<UserAccount> users = adminService.listUser(
        PageRequest.of(Math.max(userPage, 0), 5, Sort.by(safeUserDirection, safeUserSort)));
    Page<UserAccount> admins = adminService.listAdmin(
        PageRequest.of(Math.max(adminPage, 0), 5, Sort.by(safeAdminDirection, safeAdminSort)));

    model.addAttribute("userPage", users);
    model.addAttribute("listUser", users.getContent());
    model.addAttribute("userSort", safeUserSort);
    model.addAttribute("userDirection", safeUserDirection.name().toLowerCase());
    model.addAttribute("adminPage", admins);
    model.addAttribute("listAdmin", admins.getContent());
    model.addAttribute("adminSort", safeAdminSort);
    model.addAttribute("adminDirection", safeAdminDirection.name().toLowerCase());

    UserAccount user = userService.getUserLogin();
    UserProfile profile = user.getProfile();
    session.setAttribute("profileLog", profile);

    return "admin/dashboard";
  }

  // Edit User
  @GetMapping("/edit-user")
  public String editUser(@RequestParam("id") int id, Model model) {
    UserProfile profile = adminService.getProfileById(id);

    model.addAttribute("profile", profile);

    return "admin/edit-user";
  }

  @PostMapping("/editProfileProcess")
  public String saveEditUser(@Valid @ModelAttribute("profile") UserProfile profile, BindingResult bindingResult) {

    if (bindingResult.hasErrors()) {
      return "admin/edit-user";
    }

    adminService.editUser(profile);

    return "redirect:/admin/dashboard";
  }

  // Mark Admin
  @PostMapping("/mark-admin/{idUser}")
  public String markAdmin(@PathVariable("idUser") int id) {
    adminService.markAsAdmin(id);

    return "redirect:/admin/dashboard";
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
