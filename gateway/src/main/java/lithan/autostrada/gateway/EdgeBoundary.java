package lithan.autostrada.gateway;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.DefaultCorsProcessor;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Stateless edge hygiene. Never reads a request body or authenticates a user. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class EdgeBoundary implements WebFilter {
  private static final Logger log = LoggerFactory.getLogger(EdgeBoundary.class);
  private static final Set<String> PRIVATE_HEADERS = Set.of("authorization", "proxy-authorization",
      "x-role", "x-roles", "x-auth-token", "x-e2e-control", "x-original-url", "x-rewrite-url");
  private final URI publicUrl;
  private final CorsConfiguration cors = new CorsConfiguration();

  EdgeBoundary(@Value("${gateway.public-url}") URI publicUrl,
      @Value("${gateway.allowed-origins}") String origins) {
    if (!Set.of("http", "https").contains(publicUrl.getScheme()) || publicUrl.getHost() == null
        || publicUrl.getUserInfo() != null || publicUrl.getQuery() != null || publicUrl.getFragment() != null
        || !(publicUrl.getPath().isEmpty() || publicUrl.getPath().equals("/"))) {
      throw new IllegalArgumentException("gateway.public-url must be an HTTP(S) origin");
    }
    this.publicUrl = publicUrl;
    var allowed = new ArrayList<>(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
    allowed.add(publicUrl.getScheme() + "://" + publicUrl.getRawAuthority());
    if (allowed.stream().anyMatch(s -> s.contains("*"))) throw new IllegalArgumentException("Exact CORS origins required");
    cors.setAllowedOrigins(allowed);
    cors.setAllowCredentials(true);
    cors.setAllowedMethods(Arrays.asList("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cors.setAllowedHeaders(Arrays.asList("Content-Type", "X-CSRF-TOKEN", "Idempotency-Key", "X-Request-ID"));
    cors.setExposedHeaders(Arrays.asList("X-Request-ID"));
    cors.setMaxAge(600L);
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String requestId = UUID.randomUUID().toString(); // Never echo an attacker-controlled log/correlation value.
    long started = System.nanoTime();
    exchange.getResponse().getHeaders().set("X-Request-ID", requestId);
    String path = exchange.getRequest().getPath().value();
    boolean api = path.equals("/api") || path.startsWith("/api/");
    if (api && !new DefaultCorsProcessor().process(cors, exchange)) return exchange.getResponse().setComplete();
    if (api && org.springframework.web.cors.reactive.CorsUtils.isPreFlightRequest(exchange.getRequest())) {
      return exchange.getResponse().setComplete();
    }
    var request = exchange.getRequest().mutate().headers(headers -> {
      new ArrayList<>(headers.keySet()).forEach(name -> {
        String key = name.toLowerCase(Locale.ROOT);
        if (key.equals("forwarded") || key.startsWith("x-forwarded-") || key.startsWith("x-user-")
            || key.startsWith("x-internal-") || key.startsWith("x-auth-") || key.startsWith("x-role")
            || PRIVATE_HEADERS.contains(key)) headers.remove(name);
      });
      if (api) headers.remove("Origin"); // CORS has exactly one owner; backend must not add a second set.
      headers.set("Host", publicUrl.getRawAuthority());
      headers.set("X-Request-ID", requestId);
    }).build();
    var forwarded = exchange.mutate().request(request).build();
    boolean privatePath = path.startsWith("/__") || path.equals("/internal") || path.startsWith("/internal/")
        || (path.startsWith("/actuator") && !Set.of("/actuator/health", "/actuator/health/liveness",
            "/actuator/health/readiness").contains(path));
    Mono<Void> result = privatePath ? error(forwarded, HttpStatus.NOT_FOUND) : chain.filter(forwarded);
    return result.onErrorResume(error -> {
      if (exchange.getResponse().isCommitted()) return Mono.error(error);
      HttpStatusCode status = error instanceof ResponseStatusException response ? response.getStatusCode() : HttpStatus.BAD_GATEWAY;
      // Exception messages can contain upstream URLs, query strings and provider data. Do not log them.
      return error(forwarded, status);
    }).doFinally(signal -> {
      Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
      var status = exchange.getResponse().getStatusCode();
      log.info("service=gateway request_id={} route={} method={} status={} duration_ms={} completion={}",
          requestId, route == null ? "local" : route.getId(), exchange.getRequest().getMethod(),
          status == null ? 200 : status.value(), (System.nanoTime() - started) / 1_000_000, signal);
    });
  }

  static Mono<Void> error(ServerWebExchange exchange, HttpStatusCode status) {
    exchange.getResponse().setStatusCode(status);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    String message = status.value() == 404 ? "Route not found." : status.value() == 504
        ? "Upstream response timed out." : "Request could not be completed.";
    byte[] body = ("{\"message\":\"" + message + "\",\"fieldErrors\":{}}").getBytes(StandardCharsets.UTF_8);
    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
  }
}
