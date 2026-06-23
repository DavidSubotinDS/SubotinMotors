package lithan.abc.cars.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import lithan.abc.cars.entity.ProfilePicture;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.entity.UserProfile;
import lithan.abc.cars.dto.UserProfileForm;
import lithan.abc.cars.service.UserService;

@Controller
@RequestMapping("/user")
public class UserController {

  @Autowired
  private UserService userService;

  @GetMapping("")
  public String user() {
    return "redirect:/user/my-profile";
  }

  @GetMapping("/my-profile")
  public String myProfile(Model model, HttpSession session) {
    UserAccount user = userService.getUserLogin();
    UserProfile profile = user.getProfile();
    ProfilePicture picture = profile.getProfilePicture();

    model.addAttribute("profile", profile);
    model.addAttribute("email", user.getEmail());
    model.addAttribute("picture", picture);
    session.setAttribute("profileLog", profile);

    return "user/my-profile";
  }

  // Edit Profile
  @GetMapping("/edit-profile")
  public String editProfile(
      @RequestParam(name = "addressRequired", required = false) String addressRequired,
      Model model) {
    UserAccount user = userService.getUserLogin();

    model.addAttribute("profile", UserProfileForm.from(user));
    model.addAttribute("addressRequired", addressRequired != null);
    return "user/edit-profile";
  }

  @PostMapping("/editProfileProcess")
  public String saveEditProfile(@Valid @ModelAttribute("profile") UserProfileForm profile, BindingResult bindingResult,
      @RequestParam(name = "checkoutReturn", defaultValue = "false") boolean checkoutReturn,
      HttpSession session) {
    if (bindingResult.hasErrors()) {
      return "user/edit-profile";
    }

    try {
      userService.editUserProfile(profile);
    } catch (DataIntegrityViolationException exception) {
      bindingResult.rejectValue("email", "duplicate", "That email is already registered");
      return "user/edit-profile";
    }

    session.setAttribute("profileLog", userService.getUserLogin().getProfile());

    return checkoutReturn ? "redirect:/cart?addressUpdated" : "redirect:/user/my-profile";
  }

  // Upload Profile Picture
  @GetMapping("/upload-picture")
  public String uploadPicture() {
    return "user/upload-picture";
  }

  @PostMapping("/uploadPicture")
  public String uploadProfileImage(@RequestParam("imageFile") MultipartFile imageFile, Model model,
      HttpSession session) {
    UserAccount user = userService.getUserLogin();
    UserProfile profile = user.getProfile();

    try {
      userService.saveImage(imageFile, profile);
      session.setAttribute("profileLog", profile);
      return "redirect:/user/my-profile";
    } catch (IllegalArgumentException exception) {
      model.addAttribute("message", exception.getMessage());
      return "user/upload-picture";
    } catch (Exception exception) {
      model.addAttribute("message", "Unable to save image");
      return "user/upload-picture";
    }
  }

}
