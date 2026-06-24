package lithan.autostrada.auctions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ListingRouteSmokeTests {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void publicListingPagesRenderOrForwardToCanonicalListings() throws Exception {
    mockMvc.perform(get("/cars"))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("carPage", "listCar"));

    mockMvc.perform(get("/auctions"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/cars"));

    mockMvc.perform(get("/live-auctions"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/cars"));

    mockMvc.perform(get("/listings"))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("listingPage", "listings"));

    mockMvc.perform(get("/cars-for-sale"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/listings"));

    mockMvc.perform(get("/parts"))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("partPage", "parts", "categories"));

    mockMvc.perform(get("/store"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/parts"));

    mockMvc.perform(get("/store/parts"))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/parts"));
  }

  @Test
  void authenticatedUserListingPagesRenderOrForwardToCanonicalPages() throws Exception {
    mockMvc.perform(get("/user/my-posted-car").with(user("user123").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("userCar"));

    mockMvc.perform(get("/user/my-posted-cars").with(user("user123").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/user/my-posted-car"));

    mockMvc.perform(get("/user/listings").with(user("user123").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(model().attributeExists("listings"));

    mockMvc.perform(get("/user/my-listings").with(user("user123").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/user/listings"));

    mockMvc.perform(get("/user/favorites").with(user("user123").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/user/followed-auctions"));

    mockMvc.perform(get("/user/test-rides").with(user("user123").roles("USER")))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/user/test-drive"));
  }

  @Test
  void adminListingPagesRemainProtectedAndAliasesForwardForAdmins() throws Exception {
    mockMvc.perform(get("/admin/dashboard"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));

    mockMvc.perform(get("/admin/users"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));

    mockMvc.perform(get("/admin/users").with(user("admin123").roles("USER", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/admin/dashboard"));

    mockMvc.perform(get("/admin/parts").with(user("admin123").roles("USER", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(forwardedUrl("/admin/store/parts"));
  }

  @Test
  void invalidAuthenticatedRouteStillReturns404() throws Exception {
    mockMvc.perform(get("/not-a-real-listing-route").with(user("user123").roles("USER")))
        .andExpect(status().isNotFound());
  }
}
