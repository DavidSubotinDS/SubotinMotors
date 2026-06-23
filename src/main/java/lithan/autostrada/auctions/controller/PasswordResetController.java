package lithan.autostrada.auctions.controller;

import java.util.Objects;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lithan.autostrada.auctions.dto.ForgotPasswordForm;
import lithan.autostrada.auctions.dto.ResetPasswordForm;
import lithan.autostrada.auctions.service.PasswordResetService;

@Controller
public class PasswordResetController {

  private final PasswordResetService passwordResetService;

  public PasswordResetController(PasswordResetService passwordResetService) {
    this.passwordResetService = passwordResetService;
  }

  @GetMapping("/forgot-password")
  public String forgotPassword(Model model) {
    model.addAttribute("forgotPassword", new ForgotPasswordForm());
    return "forgot-password";
  }

  @PostMapping("/forgot-password")
  public String requestPasswordReset(
      @Valid @ModelAttribute("forgotPassword") ForgotPasswordForm form,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) {
      return "forgot-password";
    }
    passwordResetService.requestReset(form.getIdentifier());
    model.addAttribute(
        "message",
        "If an account matches that email or username, a reset link has been sent.");
    return "forgot-password";
  }

  @GetMapping("/reset-password")
  public String resetPasswordPage(@RequestParam(required = false) String token, Model model) {
    ResetPasswordForm form = new ResetPasswordForm();
    form.setToken(token);
    model.addAttribute("resetPassword", form);
    model.addAttribute("tokenValid", passwordResetService.isValid(token));
    return "reset-password";
  }

  @PostMapping("/reset-password")
  public String resetPassword(
      @Valid @ModelAttribute("resetPassword") ResetPasswordForm form,
      BindingResult bindingResult,
      Model model) {
    if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
      bindingResult.rejectValue(
          "confirmPassword", "mismatch", "Passwords do not match");
    }
    if (bindingResult.hasErrors()) {
      model.addAttribute("tokenValid", passwordResetService.isValid(form.getToken()));
      return "reset-password";
    }
    if (!passwordResetService.resetPassword(form.getToken(), form.getPassword())) {
      model.addAttribute("tokenValid", false);
      model.addAttribute(
          "message", "This reset link is invalid, expired, or has already been used.");
      return "reset-password";
    }
    return "redirect:/login?reset";
  }
}
