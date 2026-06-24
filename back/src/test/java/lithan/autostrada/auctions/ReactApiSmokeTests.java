package lithan.autostrada.auctions;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ReactApiSmokeTests {

  @Autowired
  private MockMvc mockMvc;

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
}
