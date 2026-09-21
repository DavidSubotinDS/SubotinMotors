package lithan.autostrada.auctions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Comparator;

import com.fasterxml.jackson.databind.ObjectMapper;
import lithan.autostrada.auctions.dto.api.ApiModels.AdminCarManagementResponse;
import lithan.autostrada.auctions.dto.api.ApiModels.AdminDashboardResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@org.springframework.context.annotation.Import(BusinessIdentityFixtures.class)
@SpringBootTest
@AutoConfigureMockMvc
class AutostradaAuctionsApplicationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void contextLoads() {
  }

  @Test
  void homePageRedirectsToReactWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("http://localhost:5173/"));
  }

  @Test
  void carCatalogueSupportsPaginationFilteringAndSorting() throws Exception {
    mockMvc.perform(get("/api/public/auctions")
            .param("page", "0").param("size", "4")
            .param("keyword", "test").param("low", "1000").param("high", "50000")
            .param("sort", "price").param("direction", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(4))
        .andExpect(jsonPath("$.content.length()", lessThanOrEqualTo(4)));
  }

  @Test
  void partsCatalogueIsPublicAndSearchable() throws Exception {
    mockMvc.perform(get("/api/public/parts")
            .param("keyword", "filter").param("category", "Filters")
            .param("sort", "priceMinor").param("direction", "asc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", not(empty())))
        .andExpect(jsonPath("$.content[*].category", everyItem(is("Filters"))));
    mockMvc.perform(get("/api/public/part-categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasItem("Filters")));
  }

  @Test
  void carCatalogueRejectsAnInvertedPriceRange() throws Exception {
    mockMvc.perform(get("/api/public/auctions").param("low", "50000").param("high", "1000"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Minimum price cannot be greater than maximum price"));
  }

  @Test
  @WithIdentity(username = "admin123", roles = "ADMIN")
  void adminListsSupportIndependentPaginationAndSorting() throws Exception {
    var carsResult = mockMvc.perform(get("/api/admin/cars")
            .param("carPage", "1").param("carSize", "2").param("bidPage", "0")
            .param("carSort", "price").param("carDirection", "asc")
            .param("bidSort", "car.make").param("bidDirection", "desc"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cars.page").value(1))
        .andExpect(jsonPath("$.cars.size").value(2))
        .andExpect(jsonPath("$.bids.page").value(0))
        .andReturn();
    var cars = objectMapper.readValue(carsResult.getResponse().getContentAsByteArray(),
        AdminCarManagementResponse.class);
    assertThat(cars.cars().content().stream().map(car -> car.price()).toList())
        .hasSize(2).isSorted();
    assertThat(cars.bids().content().stream().map(bid -> bid.auction().make()).toList())
        .isNotEmpty().isSortedAccordingTo(Comparator.reverseOrder());
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void bidAndTestDriveManagementApisAreAvailable() throws Exception {
    mockMvc.perform(get("/api/user/bids"))
        .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
    mockMvc.perform(get("/api/user/appointments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookedTestDrives").isArray())
        .andExpect(jsonPath("$.receivedTestDrives").isArray());
  }

  @Test
  @WithIdentity(username = "user123", roles = "USER")
  void retiredPaymentsRedirectAndOrdersSupportPagination() throws Exception {
    mockMvc.perform(get("/user/payments"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/orders"));
    mockMvc.perform(get("/api/store/orders").param("page", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(1))
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void userPagesRequireAuthentication() throws Exception {
    mockMvc.perform(get("/user/my-profile"))
        .andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
  }


}
