package lithan.autostrada.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
class GatewayRoutes {
  // Canonical React GET/HEAD navigation wins over overlapping legacy MVC views.
  // Every non-read action in these families still goes to the backend below.
  static final String[] SPA = { "/", "/login", "/register", "/register/thank-you",
      "/forgot-password", "/reset-password", "/about-us", "/contact-us",
      "/auctions", "/auctions/{id}", "/listings", "/listings/{id}",
      "/parts", "/parts/{id}", "/cart", "/orders", "/orders/{id}",
      "/profiles/{id}", "/store/checkout/success", "/listing-deposits/success",
      "/user/profile", "/user/profile/edit", "/user/auctions", "/user/auctions/new",
      "/user/auctions/{id}/edit", "/user/listings", "/user/listings/new", "/user/listings/{id}/edit",
      "/user/appointments", "/user/bids", "/user/followed-auctions", "/user/notifications",
      "/user/listing-deposits", "/admin/users", "/admin/cars", "/admin/cars/{id}/preview",
      "/admin/transactions", "/admin/store/parts", "/admin/store/parts/new",
      "/admin/store/parts/{id}/edit", "/admin/store/orders", "/admin/store/orders/{id}" };
  static final String[] LEGACY = { "/cars", "/cars/**", "/auctions/**", "/auction/**",
      "/live-auctions", "/browse-auctions", "/auction-listings", "/listings/**",
      "/car-listings", "/car-listings/**", "/cars-for-sale", "/cars-for-sale/**", "/vehicle-listings",
      "/parts/**", "/store", "/store/**", "/car-parts", "/car-parts/**", "/parts-store",
      "/cart/**", "/orders/**", "/user", "/user/**", "/admin", "/admin/**",
      "/register/**", "/forgot-password", "/reset-password", "/loginUser", "/logout",
      "/payments/**", "/listing-deposits/**", "/view-user/**", "/car-bid",
      "/postCarBidding", "/test-drive/**" };

  @Bean
  RouteLocator routes(RouteLocatorBuilder builder,
      @Value("${gateway.backend-url}") String backend,
      @Value("${gateway.identity-url:http://127.0.0.1:8082}") String identity,
      @Value("${gateway.notification-url:http://127.0.0.1:8083}") String notification,
      @Value("${gateway.payment-url:http://127.0.0.1:8084}") String payment,
      @Value("${gateway.frontend-url}") String frontend) {
    return builder.routes()
        .route("csrf", r -> r.path("/api/csrf").and().method(HttpMethod.GET)
            .filters(f -> f.preserveHostHeader()).uri(identity))
        .route("identity-api", r -> r.path("/api/auth/**", "/api/session", "/api/user/profile",
            "/api/user/profile/picture", "/api/admin/dashboard", "/api/admin/users/**", "/api/public/profiles/{id}")
            .filters(f -> f.preserveHostHeader()).uri(identity))
        .route("notification-api", r -> r.path("/api/user/notifications", "/api/user/notifications/**")
            .filters(f -> f.preserveHostHeader()).uri(notification))
        .route("payment-api", r -> r.path("/api/payments/**")
            .filters(f -> f.preserveHostHeader()).uri(payment))
        .route("api", r -> r.path("/api", "/api/**")
            .filters(f -> f.preserveHostHeader()).uri(backend))
        .route("stripe-webhook", r -> r.predicate(e -> "/webhooks/stripe".equals(e.getRequest().getURI().getRawPath()))
            .and().method(HttpMethod.POST)
            .filters(f -> f.preserveHostHeader()).uri(payment))
        .route("spa", r -> r.path(SPA).and().method(HttpMethod.GET, HttpMethod.HEAD).uri(frontend))
        .route("identity-legacy", r -> r.path("/loginUser", "/logout", "/register/**", "/forgot-password", "/reset-password",
            "/user", "/user/my-profile", "/user/edit-profile", "/user/editProfileProcess", "/user/upload-picture",
            "/user/uploadPicture", "/admin", "/admin/dashboard", "/admin/edit-user", "/admin/editProfileProcess", "/admin/mark-admin/{id}")
            .filters(f -> f.preserveHostHeader()).uri(identity))
        .route("notification-legacy", r -> r.path("/user/notifications/**")
            .filters(f -> f.preserveHostHeader()).uri(notification))
        .route("legacy", r -> r.path(LEGACY).filters(f -> f.preserveHostHeader()).uri(backend))
        .route("frontend-assets", r -> r.path("/assets/**", "/images/**", "/favicon.ico", "/vite.svg",
            "/@vite/**", "/@react-refresh", "/src/**", "/node_modules/**")
            .and().method(HttpMethod.GET, HttpMethod.HEAD).uri(frontend))
        .build();
  }
}
