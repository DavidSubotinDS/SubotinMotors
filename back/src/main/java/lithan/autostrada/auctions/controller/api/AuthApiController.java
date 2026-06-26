package lithan.autostrada.auctions.controller.api;

import java.util.List;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lithan.autostrada.auctions.dto.api.ApiModels.ApiMessageResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.LoginRequest;
import lithan.autostrada.auctions.dto.api.ApiModels.PasswordResetCompleteRequest;
import lithan.autostrada.auctions.dto.api.ApiModels.PasswordResetRequest;
import lithan.autostrada.auctions.dto.api.ApiModels.RegistrationRequest;
import lithan.autostrada.auctions.dto.api.UserSessionResponse;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.entity.UserProfile;
import lithan.autostrada.auctions.service.PasswordResetService;
import lithan.autostrada.auctions.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

  private final AuthenticationManager authenticationManager;
  private final PasswordResetService passwordResetService;
  private final SessionApiController sessionApiController;
  private final UserService userService;

  public AuthApiController(
      AuthenticationManager authenticationManager,
      PasswordResetService passwordResetService,
      SessionApiController sessionApiController,
      UserService userService) {
    this.authenticationManager = authenticationManager;
    this.passwordResetService = passwordResetService;
    this.sessionApiController = sessionApiController;
    this.userService = userService;
  }

  @PostMapping("/login")
  public UserSessionResponse login(
      @RequestBody LoginRequest request,
      HttpServletRequest httpRequest) {
    requireText(request.username(), "Username is required");
    requireText(request.password(), "Password is required");
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password()));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    httpRequest.getSession(true).setAttribute(
        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        context);
    return sessionApiController.session(authentication);
  }

  @PostMapping("/logout")
  public ApiMessageResponse logout(
      Authentication authentication,
      HttpServletRequest request,
      HttpServletResponse response) {
    new SecurityContextLogoutHandler().logout(request, response, authentication);
    return new ApiMessageResponse("Signed out.", null);
  }

  @PostMapping("/register")
  public ApiMessageResponse register(@Valid @RequestBody RegistrationRequest request) {
    requireText(request.username(), "Username is required");
    requireText(request.email(), "Email is required");
    requireText(request.password(), "Password is required");
    requireText(request.firstName(), "First name is required");
    requireText(request.lastName(), "Last name is required");
    requireText(request.phoneNumber(), "Phone number is required");

    UserAccount account = new UserAccount();
    account.setUsername(request.username());
    account.setEmail(request.email());
    account.setPassword(request.password());

    UserProfile profile = new UserProfile();
    profile.setFirstName(request.firstName());
    profile.setLastName(request.lastName());
    profile.setPhoneNumber(request.phoneNumber());
    profile.setAddress(request.address());
    profile.setStreetAddress(request.streetAddress());
    profile.setCity(request.city());
    profile.setPostalCode(request.postalCode());
    profile.setCountry(request.country());
    profile.setAbout(request.about());

    try {
      userService.saveUser(account, profile);
    } catch (DataIntegrityViolationException exception) {
      throw new IllegalArgumentException("That username or email is already registered.");
    }
    return new ApiMessageResponse("Account created. You can sign in now.", "/login");
  }

  @PostMapping("/password-reset")
  public ApiMessageResponse requestPasswordReset(@RequestBody PasswordResetRequest request) {
    requireText(request.identifier(), "Enter your email address or username.");
    passwordResetService.requestReset(request.identifier());
    return new ApiMessageResponse(
        "If an account matches that email or username, a reset link has been sent.",
        null);
  }

  @GetMapping("/password-reset/valid")
  public java.util.Map<String, Boolean> passwordResetValid(@RequestParam(required = false) String token) {
    return java.util.Map.of("valid", passwordResetService.isValid(token));
  }

  @PostMapping("/password-reset/complete")
  public ApiMessageResponse completePasswordReset(@RequestBody PasswordResetCompleteRequest request) {
    requireText(request.token(), "Reset token is required.");
    requireText(request.password(), "Password is required.");
    if (!Objects.equals(request.password(), request.confirmPassword())) {
      throw new IllegalArgumentException("Passwords do not match.");
    }
    if (!passwordResetService.resetPassword(request.token(), request.password())) {
      throw new IllegalArgumentException("This reset link is invalid, expired, or has already been used.");
    }
    return new ApiMessageResponse("Password updated. You can sign in now.", "/login");
  }

  private void requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }
}
