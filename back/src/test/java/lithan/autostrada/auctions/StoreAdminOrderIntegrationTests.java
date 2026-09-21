package lithan.autostrada.auctions;

import static lithan.autostrada.auctions.TestIdentity.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.StoreOrder;
import lithan.autostrada.auctions.entity.StoreOrderItem;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.repository.CarPartRepository;
import lithan.autostrada.auctions.repository.StoreOrderRepository;
import lithan.autostrada.auctions.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StoreAdminOrderIntegrationTests {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private StoreOrderRepository orderRepository;

  @Autowired
  private CarPartRepository partRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void adminStoreOrdersApiReturnsAPage() throws Exception {
    mockMvc.perform(get("/api/admin/store/orders")
            .with(user("admin123").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.content").isArray());
  }

  @Test
  void adminStoreOrderDetailsExposePurchasedItemLines() throws Exception {
    StoreOrder savedOrder = saveStoreOrderWithItem();

    mockMvc.perform(get("/api/admin/store/orders/{idOrder}", savedOrder.getIdOrder())
            .with(user("admin123").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idOrder").value(savedOrder.getIdOrder()))
        .andExpect(jsonPath("$.user.username").value("user123"))
        .andExpect(jsonPath("$.status").value("PAID"))
        .andExpect(jsonPath("$.currency").value("eur"))
        .andExpect(jsonPath("$.shippingCity").value("Novi Sad"))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].partName").value("Admin Detail Brake Pads"))
        .andExpect(jsonPath("$.items[0].sku").value("ADM-DET-001"))
        .andExpect(jsonPath("$.items[0].unitPriceMinor").value(3499))
        .andExpect(jsonPath("$.items[0].quantity").value(2))
        .andExpect(jsonPath("$.items[0].lineTotalMinor").value(6998))
        .andExpect(jsonPath("$.totalMinor").value(6998));
  }

  @Test
  void regularUsersCannotAccessAdminStoreOrderDetails() throws Exception {
    StoreOrder savedOrder = saveStoreOrderWithItem();

    mockMvc.perform(get("/api/admin/store/orders/{idOrder}", savedOrder.getIdOrder())
            .with(user("user123").roles("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void anonymousUsersAreSentToLoginForAdminStoreOrderDetails() throws Exception {
    StoreOrder savedOrder = saveStoreOrderWithItem();

    mockMvc.perform(get("/admin/store/orders/{idOrder}", savedOrder.getIdOrder()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/login"));
  }

  private StoreOrder saveStoreOrderWithItem() {
    UserAccount buyer = userRepository.findByUsername("user123").orElseThrow();
    CarPart part = partRepository.findBySkuIgnoreCase("BRK-PAD-001").orElseThrow();
    Instant timestamp = Instant.parse("2026-06-24T10:15:30Z");

    StoreOrder order = new StoreOrder();
    order.setUserId(buyer.getIdUser());
    order.setTotalMinor(6998L);
    order.setCurrency("eur");
    order.setStatus("PAID");
    order.setShippingName("Test Buyer");
    order.setShippingAddress("123 Admin Detail Street, 21000 Novi Sad, Serbia");
    order.setShippingStreetAddress("123 Admin Detail Street");
    order.setShippingCity("Novi Sad");
    order.setShippingPostalCode("21000");
    order.setShippingCountry("Serbia");
    order.setCheckoutSessionId("cs_admin_item_visibility");
    order.setPaymentIntentId("pi_admin_item_visibility");
    order.setCreatedAt(timestamp);
    order.setUpdatedAt(timestamp);
    order.setPaidAt(timestamp);

    StoreOrderItem item = new StoreOrderItem();
    item.setPart(part);
    item.setSku("ADM-DET-001");
    item.setPartName("Admin Detail Brake Pads");
    item.setUnitPriceMinor(3499L);
    item.setQuantity(2);
    order.addItem(item);

    return orderRepository.saveAndFlush(order);
  }

}
