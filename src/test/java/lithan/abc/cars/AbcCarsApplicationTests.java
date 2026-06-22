package lithan.abc.cars;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
class AbcCarsApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void homePageIsPublic() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk());
	}

	@Test
	void carCatalogueSupportsPaginationFilteringAndSorting() throws Exception {
		mockMvc.perform(get("/cars")
						.param("page", "0")
						.param("size", "4")
						.param("keyword", "test")
						.param("low", "1000")
						.param("high", "50000")
						.param("sort", "price")
						.param("direction", "asc"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("carPage"))
				.andExpect(model().attribute("sort", "price"))
				.andExpect(model().attribute("direction", "asc"));
	}

	@Test
	void partsCatalogueIsPublicAndSearchable() throws Exception {
		mockMvc.perform(get("/parts")
						.param("keyword", "filter")
						.param("category", "Filters")
						.param("sort", "priceMinor")
						.param("direction", "asc"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("partPage", "parts", "categories"))
				.andExpect(model().attribute("sort", "priceMinor"));
	}

	@Test
	void carCatalogueRejectsAnInvertedPriceRange() throws Exception {
		mockMvc.perform(get("/cars")
						.param("low", "50000")
						.param("high", "1000"))
				.andExpect(status().isOk())
				.andExpect(model().attribute(
						"searchError", "Minimum price cannot be greater than maximum price"));
	}

	@Test
	@WithMockUser(username = "admin123", roles = "ADMIN")
	void adminListsSupportIndependentPaginationAndSorting() throws Exception {
		mockMvc.perform(get("/admin/dashboard")
						.param("userSort", "profile.lastName")
						.param("userDirection", "desc")
						.param("adminSort", "username")
						.param("adminDirection", "asc"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("userPage", "adminPage"))
				.andExpect(model().attribute("userSort", "profile.lastName"))
				.andExpect(model().attribute("adminSort", "username"));

		mockMvc.perform(get("/admin/car-management")
						.param("carSort", "price")
						.param("carDirection", "asc")
						.param("bidSort", "car.make")
						.param("bidDirection", "desc"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("carPage", "bidPage"))
				.andExpect(model().attribute("carSort", "price"))
				.andExpect(model().attribute("bidSort", "car.make"));

		mockMvc.perform(get("/admin/transactions")
						.param("sort", "amountMinor")
						.param("direction", "asc"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("transactionPage", "transactions", "webhookEvents"))
				.andExpect(model().attribute("sort", "amountMinor"))
				.andExpect(model().attribute("direction", "asc"));
	}

	@Test
	@WithMockUser(username = "user123", roles = "USER")
	void bidAndTestDriveManagementPagesAreAvailable() throws Exception {
		mockMvc.perform(get("/user/bids"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("bids"));

		mockMvc.perform(get("/user/test-drive"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("bookedTestDrives", "receivedTestDrives"));
	}

	@Test
	@WithMockUser(username = "user123", roles = "USER")
	void paymentListsSupportIndependentPaginationAndSorting() throws Exception {
		mockMvc.perform(get("/user/payments"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/orders"));

		mockMvc.perform(get("/orders"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("orderPage", "orders"));
	}

	@Test
	void userPagesRequireAuthentication() throws Exception {
		mockMvc.perform(get("/user/my-profile"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("**/login"));
	}

	@Test
	void seededUserCanAuthenticate() throws Exception {
		mockMvc.perform(post("/loginUser")
						.with(csrf())
						.param("username", "user123")
						.param("password", "user123"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/user"));
	}

	@Test
	void seededAdminCanAuthenticate() throws Exception {
		mockMvc.perform(post("/loginUser")
						.with(csrf())
						.param("username", "admin123")
						.param("password", "admin123"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin"));
	}
}
