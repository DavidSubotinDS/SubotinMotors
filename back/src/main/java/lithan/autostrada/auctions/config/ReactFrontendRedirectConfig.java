package lithan.autostrada.auctions.config;

import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;

@Configuration
public class ReactFrontendRedirectConfig {

  @Bean
  public ViewResolver legacyReactViewResolver(
      @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl) {
    return new ReactRedirectViewResolver(frontendBaseUrl);
  }

  private static class ReactRedirectViewResolver implements ViewResolver {

    private static final Map<String, String> ROUTES = Map.ofEntries(
        Map.entry("home", "/"),
        Map.entry("about-us", "/about-us"),
        Map.entry("contact-us", "/contact-us"),
        Map.entry("login", "/login"),
        Map.entry("register-account", "/register"),
        Map.entry("register-profile", "/register"),
        Map.entry("thank-you", "/register/thank-you"),
        Map.entry("forgot-password", "/forgot-password"),
        Map.entry("reset-password", "/reset-password"),
        Map.entry("cars", "/auctions"),
        Map.entry("car-details", "/auctions"),
        Map.entry("listings", "/listings"),
        Map.entry("listing-details", "/listings"),
        Map.entry("listing-deposit-success", "/listing-deposits/success"),
        Map.entry("view-user", "/profiles"),
        Map.entry("store/parts", "/parts"),
        Map.entry("store/part-details", "/parts"),
        Map.entry("store/cart", "/cart"),
        Map.entry("store/orders", "/orders"),
        Map.entry("store/order-details", "/orders"),
        Map.entry("store/checkout-success", "/store/checkout/success"),
        Map.entry("user/my-profile", "/user/profile"),
        Map.entry("user/edit-profile", "/user/profile/edit"),
        Map.entry("user/upload-picture", "/user/profile"),
        Map.entry("user/my-posted-car", "/user/auctions"),
        Map.entry("user/post-car", "/user/auctions/new"),
        Map.entry("user/edit-posted-car", "/user/auctions"),
        Map.entry("user/upload-car-picture", "/user/auctions"),
        Map.entry("user/car-bid", "/auctions"),
        Map.entry("user/test-drive", "/user/appointments"),
        Map.entry("user/list-test-drive", "/user/appointments"),
        Map.entry("user/bids", "/user/bids"),
        Map.entry("user/followed-auctions", "/user/followed-auctions"),
        Map.entry("user/notifications", "/user/notifications"),
        Map.entry("user/my-listings", "/user/listings"),
        Map.entry("user/listing-form", "/user/listings"),
        Map.entry("user/listing-deposits", "/user/listing-deposits"),
        Map.entry("admin/dashboard", "/admin/users"),
        Map.entry("admin/edit-user", "/admin/users"),
        Map.entry("admin/car-management", "/admin/cars"),
        Map.entry("admin/transactions", "/admin/transactions"),
        Map.entry("admin/store-parts", "/admin/store/parts"),
        Map.entry("admin/store-part-form", "/admin/store/parts"),
        Map.entry("admin/store-orders", "/admin/store/orders"),
        Map.entry("admin/store-order-details", "/admin/store/orders"),
        Map.entry("error", "/"));

    private final String frontendBaseUrl;

    private ReactRedirectViewResolver(String frontendBaseUrl) {
      this.frontendBaseUrl = frontendBaseUrl.endsWith("/")
          ? frontendBaseUrl.substring(0, frontendBaseUrl.length() - 1)
          : frontendBaseUrl;
    }

    @Override
    public View resolveViewName(String viewName, Locale locale) {
      if (viewName == null || viewName.startsWith("redirect:") || viewName.startsWith("forward:")) {
        return null;
      }
      String route = ROUTES.getOrDefault(viewName, "/");
      RedirectView view = new RedirectView(frontendBaseUrl + route);
      view.setContextRelative(false);
      return view;
    }
  }
}
