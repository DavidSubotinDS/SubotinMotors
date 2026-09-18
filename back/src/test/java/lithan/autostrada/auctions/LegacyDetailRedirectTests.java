package lithan.autostrada.auctions;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import lithan.autostrada.auctions.config.ReactFrontendRedirectConfig;
import lithan.autostrada.auctions.entity.*;
import lithan.autostrada.auctions.dto.CarListingForm;
import lithan.autostrada.auctions.dto.CarPartForm;

class LegacyDetailRedirectTests {
  static Stream<Arguments> selectedResources() {
    Car car = new Car(); car.setIdCar(41);
    CarListing listing = new CarListing(); org.springframework.test.util.ReflectionTestUtils.setField(listing, "idListing", 42);
    CarPart part = new CarPart(); org.springframework.test.util.ReflectionTestUtils.setField(part, "idPart", 43);
    StoreOrder order = new StoreOrder(); org.springframework.test.util.ReflectionTestUtils.setField(order, "idOrder", 44);
    UserProfile profile = new UserProfile(); profile.setIdProfile(45);
    CarListingForm listingForm = new CarListingForm(); listingForm.setIdListing(46);
    CarPartForm partForm = new CarPartForm(); partForm.setIdPart(47);
    return Stream.of(
        Arguments.of("car-details", "car", car, "/auctions/41"),
        Arguments.of("listing-details", "listing", listing, "/listings/42"),
        Arguments.of("store/part-details", "part", part, "/parts/43"),
        Arguments.of("store/order-details", "order", order, "/orders/44"),
        Arguments.of("admin/store-order-details", "order", order, "/admin/store/orders/44"),
        Arguments.of("view-user", "profile", profile, "/profiles/45"),
        Arguments.of("user/edit-posted-car", "car", car, "/user/auctions/41/edit"),
        Arguments.of("user/listing-form", "listingForm", listingForm, "/user/listings/46/edit"),
        Arguments.of("admin/store-part-form", "partForm", partForm, "/admin/store/parts/47/edit"));
  }
  @ParameterizedTest @MethodSource("selectedResources")
  void selectedResourceAndQuerySurviveWithoutLeakingModel(String name, String key, Object value, String target) throws Exception {
    var resolver = new ReactFrontendRedirectConfig().legacyReactViewResolver("http://localhost:8081");
    var request = new MockHttpServletRequest("GET", "/legacy");
    request.setQueryString("search=a%2Bb&sort=price");
    var response = new MockHttpServletResponse();
    resolver.resolveViewName(name, Locale.ROOT).render(Map.of(key, value, "privateEmail", "hidden@example.invalid"), request, response);
    assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:8081" + target + "?search=a%2Bb&sort=price");
  }
}
