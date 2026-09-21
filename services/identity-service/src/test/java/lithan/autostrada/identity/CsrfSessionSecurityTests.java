package lithan.autostrada.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.identity.repository.*;
import lithan.autostrada.identity.service.PasswordResetService;

/** Exercise the production chain with tokens issued by the endpoint, not csrf(). */
@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false, print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@Transactional
class CsrfSessionSecurityTests extends IdentityTestBase {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UserRepository users;
  @Autowired PasswordResetTokenRepository resets;
  @Autowired PasswordResetService resetService;
  @Autowired PasswordEncoder encoder;
  @Autowired ProfilePictureRepository pictures;
  @Autowired jakarta.persistence.EntityManager entities;

  record Browser(MockHttpSession session, String token) { }

  Browser csrf(MockHttpSession session) throws Exception {
    var request = get("/api/csrf");
    if (session != null) request.session(session);
    var result = mvc.perform(request).andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.token").isString())
        .andExpect(jsonPath("$.username").doesNotExist()).andReturn();
    var body = json.readTree(result.getResponse().getContentAsByteArray());
    assertThat(body.size()).isEqualTo(1);
    return new Browser((MockHttpSession) result.getRequest().getSession(false), body.get("token").asText());
  }

  Browser login(Browser browser) throws Exception {
    mvc.perform(post("/api/auth/login").session(browser.session()).header("X-CSRF-TOKEN", browser.token())
            .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"demo_bidder\",\"password\":\"demo123\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.password").doesNotExist());
    return csrf(browser.session());
  }

  @Test void anonymousTokenIsSessionBoundAndReadDoesNotRotateSessionOrExpectedToken() throws Exception {
    var a = csrf(null);
    var b = csrf(null);
    var reread = csrf(a.session());
    assertThat(reread.session()).isSameAs(a.session());
    mvc.perform(get("/api/session").session(a.session())).andExpect(jsonPath("$.authenticated").value(false));
    mvc.perform(post("/api/auth/password-reset").session(a.session()).header("X-CSRF-TOKEN", b.token())
            .contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"demo_bidder\"}"))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    // A prior masked representation remains valid until the underlying token rotates.
    mvc.perform(post("/api/auth/password-reset").session(a.session()).header("X-CSRF-TOKEN", a.token())
            .contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"no_such_account\"}"))
        .andExpect(status().isOk());
  }

  @ParameterizedTest
  @ValueSource(strings = {"/api/auth/login", "/api/auth/register", "/api/auth/logout",
      "/api/auth/password-reset", "/api/auth/password-reset/complete", "/api/store/cart/items",
      "/api/store/checkout", "/api/user/listings/1/deposit", "/api/user/auctions/1/bid",
      "/api/user/auctions/1/follow", "/api/user/notifications/read-all", "/api/admin/users/1/mark-admin"})
  void missingAndInvalidTokensFailBeforeControllers(String path) throws Exception {
    var browser = csrf(null);
    long accounts = users.count(), resetCount = resets.count();
    for (String token : new String[] {"", "invalid"}) {
      var request = post(path).session(browser.session()).contentType(MediaType.APPLICATION_JSON).content("{}");
      if (!token.isEmpty()) request.header("X-CSRF-TOKEN", token);
      mvc.perform(request).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }
    assertThat(users.count()).isEqualTo(accounts);
    assertThat(resets.count()).isEqualTo(resetCount);
    assertThat(browser.session().getAttribute("SPRING_SECURITY_CONTEXT")).isNull();
  }

  @Test void apiLoginRotatesSessionClearsAnonymousTokenAndLogoutInvalidatesSession() throws Exception {
    var anonymous = csrf(null);
    String oldId = anonymous.session().getId();
    anonymous.session().setAttribute("preserved", "value");
    var signedIn = login(anonymous);
    assertThat(signedIn.session().getId()).isNotEqualTo(oldId);
    assertThat(signedIn.session().getAttribute("preserved")).isEqualTo("value");
    mvc.perform(post("/api/auth/logout").session(signedIn.session()).header("X-CSRF-TOKEN", anonymous.token()))
        .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    mvc.perform(get("/api/session").session(signedIn.session())).andExpect(jsonPath("$.username").value("demo_bidder"));
    mvc.perform(post("/api/auth/logout").session(signedIn.session()).header("X-CSRF-TOKEN", signedIn.token()))
        .andExpect(status().isOk());
    assertThat(signedIn.session().isInvalid()).isTrue();
    var after = csrf(null);
    mvc.perform(post("/api/auth/login").session(after.session()).header("X-CSRF-TOKEN", signedIn.token())
            .contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/user/profile")).andExpect(status().isUnauthorized());
  }

  @Test void formLoginAndLogoutUseFrameworkRotationAndBodyTokenWhileGetThankYouIsReadOnly() throws Exception {
    var anonymous = csrf(null);
    String oldId = anonymous.session().getId();
    mvc.perform(post("/loginUser").session(anonymous.session()).contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("username", "demo_bidder").param("password", "demo123"))
        .andExpect(status().isForbidden());
    assertThat(anonymous.session().getId()).isEqualTo(oldId);
    mvc.perform(post("/loginUser").session(anonymous.session()).contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .param("_csrf", anonymous.token()).param("username", "demo_bidder").param("password", "demo123"))
        .andExpect(status().isFound()).andExpect(redirectedUrl("/user"));
    assertThat(anonymous.session().getId()).isNotEqualTo(oldId);
    mvc.perform(get("/register/thank-you").session(anonymous.session())).andExpect(status().isFound());
    assertThat(anonymous.session().isInvalid()).isFalse();
    mvc.perform(post("/logout").session(anonymous.session()).param("_csrf", anonymous.token()))
        .andExpect(status().isForbidden());
    var signedIn = csrf(anonymous.session());
    mvc.perform(post("/logout").session(signedIn.session()).param("_csrf", signedIn.token()))
        .andExpect(status().isFound());
    assertThat(signedIn.session().isInvalid()).isTrue();
  }

  @Test void legacyRegistrationInvalidatesOnlyAfterProtectedSuccessfulPost() throws Exception {
    var browser = csrf(null);
    mvc.perform(post("/register/accountProcess").session(browser.session())
            .param("username", "legacy_csrf").param("email", "legacy-csrf@example.invalid").param("password", "secret123"))
        .andExpect(status().isForbidden());
    assertThat(browser.session().getAttribute("registerAccount")).isNull();
    mvc.perform(post("/register/accountProcess").session(browser.session()).param("_csrf", browser.token())
            .param("username", "legacy_csrf").param("email", "legacy-csrf@example.invalid").param("password", "secret123"))
        .andExpect(status().isFound()).andExpect(redirectedUrl("/register/profile"));
    mvc.perform(post("/register/profileProcess").session(browser.session())
            .param("firstName", "Legacy").param("lastName", "Driver").param("phoneNumber", "+381601234567"))
        .andExpect(status().isForbidden());
    assertThat(users.findByUsername("legacy_csrf")).isEmpty();
    mvc.perform(post("/register/profileProcess").session(browser.session()).param("_csrf", browser.token())
            .param("firstName", "Legacy").param("lastName", "Driver").param("phoneNumber", "+381601234567"))
        .andExpect(status().isFound()).andExpect(redirectedUrl("/register/thank-you"));
    assertThat(users.findByUsername("legacy_csrf")).isPresent();
    assertThat(browser.session().isInvalid()).isTrue();
  }

  @Test void legacyResetFormsRequireBodyTokenAndPreserveRedirectContract() throws Exception {
    var browser = csrf(null);
    long count = resets.count();
    mvc.perform(post("/forgot-password").session(browser.session()).param("identifier", "demo_bidder"))
        .andExpect(status().isForbidden());
    assertThat(resets.count()).isEqualTo(count);
    mvc.perform(post("/forgot-password").session(browser.session()).param("_csrf", browser.token())
            .param("identifier", "demo_bidder")).andExpect(status().isFound());
    String reset = resetService.requestReset("demo_bidder").orElseThrow();
    mvc.perform(post("/reset-password").session(browser.session()).param("token", reset)
            .param("password", "changed123").param("confirmPassword", "changed123"))
        .andExpect(status().isForbidden());
    assertThat(resetService.isValid(reset)).isTrue();
    mvc.perform(post("/reset-password").session(browser.session()).param("_csrf", browser.token()).param("token", reset)
            .param("password", "changed123").param("confirmPassword", "changed123"))
        .andExpect(status().isFound()).andExpect(redirectedUrl("/login?reset"));
    assertThat(resetService.isValid(reset)).isFalse();
    assertThat(encoder.matches("changed123", users.findByUsername("demo_bidder").orElseThrow().getPassword())).isTrue();
  }

  @Test void registrationAndResetRequireRealSessionTokensAndRejectedConsumptionDoesNotChangePassword() throws Exception {
    var browser = csrf(null);
    var registration = Map.of("username", "csrf_driver", "email", "csrf@example.invalid", "password", "secret123",
        "firstName", "Csrf", "lastName", "Driver", "phoneNumber", "+381601234567");
    byte[] body = json.writeValueAsBytes(registration);
    mvc.perform(post("/api/auth/register").session(browser.session()).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isForbidden());
    assertThat(users.findByUsername("csrf_driver")).isEmpty();
    mvc.perform(post("/api/auth/register").session(browser.session()).header("X-CSRF-TOKEN", browser.token())
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
    // Match real separate HTTP transactions; reload the newly persisted aggregate.
    entities.flush();
    entities.clear();
    var account = users.findByUsername("csrf_driver").orElseThrow();
    assertThat(encoder.matches("secret123", account.getPassword())).isTrue();
    mvc.perform(post("/api/auth/password-reset").session(browser.session()).header("X-CSRF-TOKEN", browser.token())
            .contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"csrf_driver\"}"))
        .andExpect(status().isOk());
    String reset = resetService.requestReset("csrf_driver").orElseThrow();
    byte[] complete = json.writeValueAsBytes(Map.of("token", reset, "password", "updated123", "confirmPassword", "updated123"));
    mvc.perform(post("/api/auth/password-reset/complete").session(browser.session())
            .contentType(MediaType.APPLICATION_JSON).content(complete)).andExpect(status().isForbidden());
    assertThat(resetService.isValid(reset)).isTrue();
    assertThat(encoder.matches("secret123", account.getPassword())).isTrue();
    mvc.perform(post("/api/auth/password-reset/complete").session(browser.session()).header("X-CSRF-TOKEN", browser.token())
            .contentType(MediaType.APPLICATION_JSON).content(complete)).andExpect(status().isOk());
    assertThat(resetService.isValid(reset)).isFalse();
    assertThat(encoder.matches("updated123", account.getPassword())).isTrue();
  }



  @ParameterizedTest @ValueSource(strings = {"/webhooks/stripe/extra", "/webhooks/stripe-like", "/api/webhooks/stripe", "/webhooks/stripe/"})
  void webhookLikePathsAreNotCsrfExempt(String path) throws Exception {
    mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isForbidden());
  }


  private MockMultipartFile image() {
    return new MockMultipartFile("imageFile", "profile.png", "image/png", Base64.getDecoder().decode(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="));
  }
}
