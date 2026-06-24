package lithan.autostrada.auctions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.entity.CarListingStatus;
import lithan.autostrada.auctions.entity.CarPart;
import lithan.autostrada.auctions.entity.CartItem;
import lithan.autostrada.auctions.entity.ListingDeposit;
import lithan.autostrada.auctions.entity.ListingTestRide;
import lithan.autostrada.auctions.entity.StoreOrder;
import lithan.autostrada.auctions.entity.TestDriveStatus;
import lithan.autostrada.auctions.entity.UserAccount;
import lithan.autostrada.auctions.entity.UserProfile;
import lithan.autostrada.auctions.payment.StripeCheckoutResult;
import lithan.autostrada.auctions.payment.StripeGateway;
import lithan.autostrada.auctions.repository.CarListingRepository;
import lithan.autostrada.auctions.repository.CarPartRepository;
import lithan.autostrada.auctions.repository.CartItemRepository;
import lithan.autostrada.auctions.repository.ListingDepositRepository;
import lithan.autostrada.auctions.repository.ListingTestRideRepository;
import lithan.autostrada.auctions.repository.StoreOrderRepository;
import lithan.autostrada.auctions.repository.UserProfileRepository;
import lithan.autostrada.auctions.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CarListingAndAddressIntegrationTests {

  private static final byte[] PNG = Base64.getDecoder().decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserProfileRepository profileRepository;

  @Autowired
  private CarPartRepository partRepository;

  @Autowired
  private CartItemRepository cartItemRepository;

  @Autowired
  private StoreOrderRepository orderRepository;

  @Autowired
  private CarListingRepository listingRepository;

  @Autowired
  private ListingTestRideRepository listingTestRideRepository;

  @Autowired
  private ListingDepositRepository depositRepository;

  @MockitoBean
  private StripeGateway stripeGateway;

  @BeforeEach
  void configureStripeSandbox() {
    when(stripeGateway.isEnabled()).thenReturn(true);
    when(stripeGateway.createStoreCheckoutSession(any(StoreOrder.class)))
        .thenAnswer(invocation -> {
          StoreOrder order = invocation.getArgument(0);
          return new StripeCheckoutResult(
              "cs_address_" + order.getIdOrder(),
              "https://checkout.stripe.test/store/" + order.getIdOrder());
        });
    when(stripeGateway.createListingDepositCheckoutSession(any(ListingDeposit.class)))
        .thenAnswer(invocation -> {
          ListingDeposit deposit = invocation.getArgument(0);
          return new StripeCheckoutResult(
              "cs_deposit_" + deposit.getIdDeposit(),
              "https://checkout.stripe.test/deposit/" + deposit.getIdDeposit());
        });
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
  void partsCheckoutPromptsForMissingAddressWithoutCreatingOrder() throws Exception {
    UserAccount buyer = userRepository.findByUsername("user123").orElseThrow();
    clearAddress(buyer.getProfile());
    addCartItem(buyer, "FLT-OIL-101");
    long orderCount = orderRepository.count();

    mockMvc.perform(post("/store/checkout")
            .with(user("user123").roles("USER"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/user/edit-profile?addressRequired"));

    assertEquals(orderCount, orderRepository.count());
    assertEquals(1, cartItemRepository.countByUser(buyer));
  }

  @Test
  void partsCheckoutUsesStructuredShippingSnapshotWhenAddressExists() throws Exception {
    UserAccount buyer = userRepository.findByUsername("user123").orElseThrow();
    setAddress(buyer.getProfile(), "44 River Road", "Szeged", "6720", "Hungary");
    addCartItem(buyer, "FLT-CAB-220");

    mockMvc.perform(post("/store/checkout")
            .with(user("user123").roles("USER"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection());

    StoreOrder order = orderRepository.findAll().stream()
        .max((left, right) -> Integer.compare(left.getIdOrder(), right.getIdOrder()))
        .orElseThrow();
    assertEquals("44 River Road", order.getShippingStreetAddress());
    assertEquals("Szeged", order.getShippingCity());
    assertEquals("6720", order.getShippingPostalCode());
    assertEquals("Hungary", order.getShippingCountry());
    assertEquals("44 River Road, 6720 Szeged, Hungary", order.getShippingAddress());
  }

  @Test
  void userCanCreateListingAndVisitorsCanViewDetails() throws Exception {
    CarListing listing = createListingAs(
        "user123", "City-friendly hatchback", "Honda", "Jazz");

    mockMvc.perform(get("/listings/{listingId}", listing.getIdListing()))
        .andExpect(status().isOk())
        .andExpect(view().name("listing-details"))
        .andExpect(model().attribute("listing",
            org.hamcrest.Matchers.hasProperty(
                "idListing", org.hamcrest.Matchers.is(listing.getIdListing()))));

    assertEquals(CarListingStatus.ACTIVE, listing.getStatus());
    assertEquals("user123", listing.getSeller().getUsername());
    assertNotNull(listing.getPicture());
  }

  @Test
  void listingTestRideRejectsPastTimeAndPersistsFutureRequest() throws Exception {
    CarListing listing = createListingAs(
        "admin123", "Test ride estate", "Volvo", "V60");
    long count = listingTestRideRepository.count();

    mockMvc.perform(post("/listings/{listingId}/test-rides", listing.getIdListing())
            .with(user("user123").roles("USER"))
            .with(csrf())
            .param("scheduledAt", LocalDateTime.now().minusHours(1).withSecond(0).withNano(0).toString()))
        .andExpect(status().isOk())
        .andExpect(view().name("listing-details"))
        .andExpect(model().attributeHasFieldErrors("testRide", "scheduledAt"));
    assertEquals(count, listingTestRideRepository.count());

    LocalDateTime future = LocalDateTime.now().plusDays(3).withSecond(0).withNano(0);
    mockMvc.perform(post("/listings/{listingId}/test-rides", listing.getIdListing())
            .with(user("user123").roles("USER"))
            .with(csrf())
            .param("scheduledAt", future.toString()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/listings/" + listing.getIdListing()));

    ListingTestRide ride = listingTestRideRepository
        .findByUserOrderByScheduledAtAsc(
            userRepository.findByUsername("user123").orElseThrow())
        .stream()
        .filter(candidate -> candidate.getListing().getIdListing() == listing.getIdListing())
        .findFirst()
        .orElseThrow();
    assertEquals(TestDriveStatus.PENDING, ride.getStatus());
    assertEquals(future, ride.getScheduledAt());
  }

  @Test
  void depositCheckoutCreatesReservationWithoutFullPurchasePayment() throws Exception {
    CarListing listing = createListingAs(
        "admin123", "Reservable coupe", "BMW", "420i");

    mockMvc.perform(post("/user/listings/{listingId}/deposit", listing.getIdListing())
            .with(user("user123").roles("USER"))
            .with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("https://checkout.stripe.test/deposit/*"));

    ListingDeposit deposit = depositRepository.findAll().stream()
        .max((left, right) -> Integer.compare(left.getIdDeposit(), right.getIdDeposit()))
        .orElseThrow();
    assertEquals("CHECKOUT_CREATED", deposit.getStatus());
    assertEquals(listing.getDepositAmountMinor(), deposit.getAmountMinor());
    assertEquals("user123", deposit.getBuyer().getUsername());
    assertEquals(CarListingStatus.RESERVED,
        listingRepository.findById(listing.getIdListing()).orElseThrow().getStatus());
  }

  @Test
  void nonOwnerCannotEditListingOrManageSellerTestRideRequest() throws Exception {
    CarListing listing = createListingAs(
        "admin123", "Protected listing", "Audi", "A4");

    mockMvc.perform(get("/user/listings/{listingId}/edit", listing.getIdListing())
            .with(user("user123").roles("USER")))
        .andExpect(status().isForbidden());

    LocalDateTime future = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
    UserAccount requester = userRepository.findByUsername("admin123").orElseThrow();
    ListingTestRide ride = new ListingTestRide();
    ride.setListing(listing);
    ride.setUser(requester);
    ride.setScheduledAt(future);
    ride.setStatus(TestDriveStatus.PENDING);
    ride.setCreatedAt(Instant.now());
    ride.setUpdatedAt(Instant.now());
    listingTestRideRepository.saveAndFlush(ride);

    mockMvc.perform(post("/user/listing-test-rides/{idTestRide}/accept", ride.getIdTestRide())
            .with(user("user123").roles("USER"))
            .with(csrf()))
        .andExpect(status().isForbidden());
  }

  private CarListing createListingAs(
      String username, String title, String make, String model) throws Exception {
    MockMultipartFile image = new MockMultipartFile(
        "imageFile", "listing.png", "image/png", PNG);
    mockMvc.perform(multipart("/user/listings")
            .file(image)
            .with(user(username).roles("USER"))
            .with(csrf())
            .param("title", title)
            .param("make", make)
            .param("model", model)
            .param("year", "2024")
            .param("mileage", "25000")
            .param("fuelType", "Petrol")
            .param("transmission", "Automatic")
            .param("price", "25000.00")
            .param("depositAmount", "1000.00")
            .param("description", "A carefully maintained car with complete service history."))
        .andExpect(status().is3xxRedirection());

    UserAccount seller = userRepository.findByUsername(username).orElseThrow();
    return listingRepository.findBySellerOrderByCreatedAtDesc(seller).stream()
        .filter(listing -> title.equals(listing.getTitle()))
        .findFirst()
        .orElseThrow();
  }

  private void addCartItem(UserAccount buyer, String sku) {
    CarPart part = partRepository.findBySkuIgnoreCase(sku).orElseThrow();
    Instant now = Instant.now();
    CartItem item = new CartItem();
    item.setUser(buyer);
    item.setPart(part);
    item.setQuantity(1);
    item.setCreatedAt(now);
    item.setUpdatedAt(now);
    cartItemRepository.saveAndFlush(item);
  }

  private void clearAddress(UserProfile profile) {
    setAddress(profile, null, null, null, null);
  }

  private void setAddress(
      UserProfile profile,
      String street,
      String city,
      String postalCode,
      String country) {
    profile.setStreetAddress(street);
    profile.setCity(city);
    profile.setPostalCode(postalCode);
    profile.setCountry(country);
    profileRepository.saveAndFlush(profile);
  }
}
