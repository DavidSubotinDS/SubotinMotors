package lithan.autostrada.auctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
  void adminStoreOrderPageReturnsOk() throws Exception {
    mockMvc.perform(get("/admin/store/orders")
            .with(user("admin123").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/store-orders"))
        .andExpect(model().attributeExists("orderPage", "orders"));
  }

  @Test
  void adminStoreOrderDetailsExposePurchasedItemLines() throws Exception {
    StoreOrder savedOrder = saveStoreOrderWithItem();

    MvcResult result = mockMvc.perform(get("/admin/store/orders/{idOrder}", savedOrder.getIdOrder())
            .with(user("admin123").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(view().name("admin/store-order-details"))
        .andExpect(model().attributeExists("order"))
        .andReturn();

    StoreOrder order = orderFrom(result);
    assertEquals(savedOrder.getIdOrder(), order.getIdOrder());
    assertEquals("user123", order.getUser().getUsername());
    assertEquals("PAID", order.getStatus());
    assertEquals("pi_admin_item_visibility", order.getPaymentIntentId());
    assertEquals("cs_admin_item_visibility", order.getCheckoutSessionId());
    assertEquals(1, order.getItems().size());

    StoreOrderItem item = order.getItems().get(0);
    assertEquals("Admin Detail Brake Pads", item.getPartName());
    assertEquals("ADM-DET-001", item.getSku());
    assertEquals(3499L, item.getUnitPriceMinor());
    assertEquals(2, item.getQuantity());
    assertEquals(6998L, item.getLineTotalMinor());
    assertEquals(6998L, order.getTotalMinor());
  }

  @Test
  void regularUsersCannotAccessAdminStoreOrderDetails() throws Exception {
    StoreOrder savedOrder = saveStoreOrderWithItem();

    mockMvc.perform(get("/admin/store/orders/{idOrder}", savedOrder.getIdOrder())
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
    order.setUser(buyer);
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

  private StoreOrder orderFrom(MvcResult result) {
    assertNotNull(result.getModelAndView());
    return (StoreOrder) result.getModelAndView().getModel().get("order");
  }
}
