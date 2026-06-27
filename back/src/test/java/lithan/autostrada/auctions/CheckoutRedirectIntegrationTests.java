package lithan.autostrada.auctions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.StoreOrder;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.repository.StoreOrderRepository;
import lithan.autostrada.auctions.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CheckoutRedirectIntegrationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private StoreOrderRepository orderRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void checkoutSuccessRedirectPreservesStripeSessionIdForReact() throws Exception {
    UserAccount buyer = userRepository.findByUsername("user123").orElseThrow();
    StoreOrder order = new StoreOrder();
    order.setUser(buyer);
    order.setTotalMinor(4999L);
    order.setCurrency("eur");
    order.setStatus("CHECKOUT_CREATED");
    order.setShippingName("Test Buyer");
    order.setShippingAddress("1 Test Street, Novi Sad, Serbia");
    order.setShippingStreetAddress("1 Test Street");
    order.setShippingCity("Novi Sad");
    order.setShippingPostalCode("21000");
    order.setShippingCountry("Serbia");
    order.setCheckoutSessionId("cs_checkout_return");
    order.setCreatedAt(Instant.parse("2026-06-26T10:00:00Z"));
    order.setUpdatedAt(Instant.parse("2026-06-26T10:00:00Z"));
    orderRepository.saveAndFlush(order);

    mockMvc.perform(get("/store/checkout/success?session_id=" + order.getCheckoutSessionId())
            .with(user("user123").roles("USER")))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl(
            "http://localhost:5173/store/checkout/success?session_id=cs_checkout_return"));
  }
}
