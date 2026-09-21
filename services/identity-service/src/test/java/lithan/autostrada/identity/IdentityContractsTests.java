package lithan.autostrada.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static lithan.autostrada.identity.TestIdentity.user;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lithan.autostrada.identity.repository.*;
import lithan.autostrada.identity.entity.*;
import java.util.*;
@SpringBootTest @AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE) @Transactional
class IdentityContractsTests extends IdentityTestBase {
 @Autowired MockMvc mockMvc;
 @Autowired ObjectMapper objectMapper;
 @Autowired UserRepository userRepository;
 @Autowired UserProfileRepository profileRepository;
 @Autowired PasswordEncoder passwordEncoder;

  @Test
  void seededUserCanAuthenticate() throws Exception {
    mockMvc.perform(post("/loginUser").with(csrf())
            .param("username", "user123").param("password", "user123"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/user"));
  }
  @Test
  void seededAdminCanAuthenticate() throws Exception {
    mockMvc.perform(post("/loginUser").with(csrf())
            .param("username", "admin123").param("password", "admin123"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin"));
  }
  @Test
  void addressIsOptionalDuringRegistration() throws Exception {
    MvcResult accountResult = mockMvc.perform(post("/register/accountProcess")
            .with(csrf())
            .param("username", "noaddress")
            .param("email", "noaddress@example.com")
            .param("password", "secret123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/register/profile"))
        .andReturn();

    MockHttpSession session = (MockHttpSession) accountResult.getRequest().getSession(false);
    assertNotNull(session);

    mockMvc.perform(post("/register/profileProcess")
            .session(session)
            .with(csrf())
            .param("firstName", "No")
            .param("lastName", "Address")
            .param("phoneNumber", "0612345678"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/register/thank-you"));

    UserProfile profile = userRepository.findByUsername("noaddress").orElseThrow().getProfile();
    assertFalse(profile.hasCompleteShippingAddress());
  }
  @Test
  void profileCanAddAndUpdateStructuredAddress() throws Exception {
    UserAccount userAccount = userRepository.findByUsername("user123").orElseThrow();

    mockMvc.perform(post("/user/editProfileProcess")
            .with(user("user123").roles("USER"))
            .with(csrf())
            .param("idProfile", Integer.toString(userAccount.getProfile().getIdProfile()))
            .param("email", userAccount.getEmail())
            .param("firstName", userAccount.getProfile().getFirstName())
            .param("lastName", userAccount.getProfile().getLastName())
            .param("phoneNumber", userAccount.getProfile().getPhoneNumber())
            .param("streetAddress", "12 Market Street")
            .param("city", "Budapest")
            .param("postalCode", "1051")
            .param("country", "Hungary")
            .param("about", "Updated profile"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/user/my-profile"));

    UserProfile updated = profileRepository.findById(
        userAccount.getProfile().getIdProfile()).orElseThrow();
    assertTrue(updated.hasCompleteShippingAddress());
    assertEquals("12 Market Street, 1051 Budapest, Hungary",
        updated.getFormattedShippingAddress());
  }
  @Test
  void sessionEndpointReturnsAnonymousOrSanitizedAuthenticatedUser() throws Exception {
    mockMvc.perform(get("/api/session"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false))
        .andExpect(jsonPath("$.roles.length()").value(0));

    mockMvc.perform(get("/api/session").with(user("demo_bidder").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.username").value("demo_bidder"))
        .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")))
        .andExpect(jsonPath("$.email").doesNotExist());
  }
  @Test
  void registrationLoginAndLogoutWorkThroughTheApiSession() throws Exception {
    mockMvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(registration())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.redirectUrl").value("/login"));

    var account = userRepository.findByUsername("api_driver").orElseThrow();
    assertThat(passwordEncoder.matches("secret123", account.getPassword())).isTrue();
    assertThat(account.getEmail()).isEqualTo("api-driver@example.com");
    assertThat(account.getRoles()).extracting(role -> role.getRole()).containsExactly("ROLE_USER");
    assertThat(account.getProfile().getFirstName()).isEqualTo("Api");

    var login = mockMvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"api_driver\",\"password\":\"secret123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andReturn();
    var session = (MockHttpSession) login.getRequest().getSession(false);
    assertThat(session).isNotNull();
    mockMvc.perform(get("/api/user/profile").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("api_driver"));
    mockMvc.perform(post("/api/auth/logout").with(csrf()).session(session))
        .andExpect(status().isOk());
    assertThat(session.isInvalid()).isTrue();
    mockMvc.perform(get("/api/user/profile"))
        .andExpect(status().isUnauthorized());
  }
  @ParameterizedTest
  @CsvSource({ "password,123", "firstName,''", "lastName,''", "phoneNumber,abc" })
  void invalidRegistrationFieldsAreRejectedBeforePersistence(String field, String value) throws Exception {
    long count = userRepository.count();
    var request = registration();
    request.put(field, value);
    mockMvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors." + field).isNotEmpty());
    assertThat(userRepository.count()).isEqualTo(count);
  }  private Map<String, String> registration() {
    return new LinkedHashMap<>(Map.of(
        "username", "api_driver", "email", "api-driver@example.com", "password", "secret123",
        "firstName", "Api", "lastName", "Driver", "phoneNumber", "+381601234567"));
  }
}
