package lithan.autostrada.auctions;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import lithan.autostrada.auctions.repository.CarRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ReactApiSmokeTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CarRepository carRepository;

  @Test
  void publicMarketplaceSummaryIsAvailableForReact() throws Exception {
    mockMvc.perform(get("/api/public/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.featuredAuctions.length()", greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.fixedPriceListings.length()", greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.storeParts.length()", greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.partCategories.length()", greaterThanOrEqualTo(1)))
        .andExpect(jsonPath("$.featuredAuctions[0].sellerDisplayName", not(nullValue())));
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
  void apiAuthenticationFailuresReturnJsonErrors() throws Exception {
    mockMvc.perform(post("/api/auth/login").with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"admin123\",\"password\":\"wrong\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Invalid username or password."));

    mockMvc.perform(get("/api/user/workspace").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Authentication required."));
  }

  @ParameterizedTest
  @ValueSource(strings = { "*/*", "text/html", "application/json" })
  void authenticationResponseDependsOnRouteRatherThanAcceptHeader(String accept) throws Exception {
    mockMvc.perform(get("/api/user/workspace").accept(accept))
        .andExpect(status().isUnauthorized())
        .andExpect(header().doesNotExist("Location"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("Authentication required."));
    mockMvc.perform(get("/user/my-profile").accept(accept))
        .andExpect(status().isFound())
        .andExpect(redirectedUrlPattern("**/login"));
    mockMvc.perform(get("/api/admin/transactions").accept(accept)
            .with(user("user123").roles("USER")))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Location"))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("You do not have permission to perform this action."));
  }

  @Test
  void adminApisExposeAuctionPreviewsAndPartVisibility() throws Exception {
    int pendingCarId = carRepository.findAll().stream()
        .filter(car -> "PENDING".equals(car.getStatus()))
        .findFirst()
        .orElseThrow()
        .getIdCar();

    mockMvc.perform(get("/api/admin/cars/{idCar}", pendingCarId)
            .with(user("admin123").roles("USER", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.auction.id").value(pendingCarId))
        .andExpect(jsonPath("$.auction.status").value("PENDING"))
        .andExpect(jsonPath("$.auction.sellerDisplayName", not(nullValue())));

    mockMvc.perform(get("/api/admin/store/parts")
            .with(user("admin123").roles("USER", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].active").isBoolean());
  }
}
