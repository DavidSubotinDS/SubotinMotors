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
      @Value("${gateway.frontend-url}") String frontend) {
    return builder.routes()
        .route("api", r -> r.path("/api", "/api/**")
            .filters(f -> f.preserveHostHeader()).uri(backend))
        .route("stripe-webhook", r -> r.predicate(e -> "/webhooks/stripe".equals(e.getRequest().getURI().getRawPath()))
            .and().method(HttpMethod.POST)
            .filters(f -> f.preserveHostHeader()).uri(backend))
        .route("spa", r -> r.path(SPA).and().method(HttpMethod.GET, HttpMethod.HEAD).uri(frontend))
        .route("legacy", r -> r.path(LEGACY).filters(f -> f.preserveHostHeader()).uri(backend))
        .route("frontend-assets", r -> r.path("/assets/**", "/images/**", "/favicon.ico", "/vite.svg",
            "/@vite/**", "/@react-refresh", "/src/**", "/node_modules/**")
            .and().method(HttpMethod.GET, HttpMethod.HEAD).uri(frontend))
        .build();
  }
}
