package lithan.autostrada.auctions.controller;

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

import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.entity.UserProfile;
import lithan.autostrada.auctions.service.UserService;

@Controller
@RequestMapping("/register")
public class RegisterController {

  @Autowired
  private UserService userService;

  @GetMapping("")
  public String register() {

    return "redirect:/register/account";
  }

  // Register Account
  @GetMapping("/account")
  public String registerAccount(Model model) {
    UserAccount user = new UserAccount();

    model.addAttribute("account", user);

    return "register-account";
  }

  @PostMapping("/accountProcess")
  public String registerAccountProcess(
      @Valid @ModelAttribute("account") UserAccount user,
      BindingResult bindingResult, HttpSession session) {

    if (bindingResult.hasErrors()) {
      return "register-account";
    }

    session.setAttribute("registerAccount", user);

    return "redirect:/register/profile";
  }

  // Register Profile
  @GetMapping("/profile")
  public String registerProfile(Model model) {
    UserProfile profile = new UserProfile();

    model.addAttribute("profile", profile);

    return "register-profile";
  }

  @PostMapping("/profileProcess")
  public String registerProfileProcess(
      @Valid @ModelAttribute("profile") UserProfile profile,
      BindingResult bindingResult, HttpSession session) {

    if (bindingResult.hasErrors()) {
      return "register-profile";
    }

    UserAccount user = (UserAccount) session.getAttribute("registerAccount");

    if (user == null) {
      return "redirect:/register/account";
    }
    try {
      userService.saveUser(user, profile);
    } catch (DataIntegrityViolationException exception) {
      session.removeAttribute("registerAccount");
      String duplicate = exception.getMessage() != null
          && exception.getMessage().toLowerCase().contains("email")
              ? "email" : "username";
      return "redirect:/register/account?duplicate=" + duplicate;
    }

    return "redirect:/register/thank-you";
  }

  // Thank You
  @GetMapping("thank-you")
  public String thankYou(HttpSession session) {
    session.invalidate();

    return "thank-you";
  }
}
