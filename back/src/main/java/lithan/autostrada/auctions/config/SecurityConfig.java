package lithan.autostrada.auctions.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private static final RequestMatcher API_REQUEST =
      request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/");
  private static final RequestMatcher STRIPE_WEBHOOK = request ->
      "POST".equals(request.getMethod())
          && (request.getContextPath() + "/webhooks/stripe").equals(request.getRequestURI());

  @Value("${app.cors.allowed-origins:http://localhost:5173}")
  private List<String> allowedCorsOrigins;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http.cors(cors -> { });
    http.sessionManagement(s -> s.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS));
    http.securityContext(s -> s.securityContextRepository(new org.springframework.security.web.context.NullSecurityContextRepository()));
    http.requestCache(c -> c.disable());
    http.csrf(c -> c.disable()); // Cookie authentication removed. Gateway exchange validates browser CSRF.
    http.formLogin(c -> c.disable()); http.logout(c -> c.disable()); http.httpBasic(c -> c.disable());
    http.oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(AssertionSecurity.converter())));


    // Authorize
    http.authorizeHttpRequests(configurer -> configurer
        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
        .requestMatchers("/css/**", "/images/**", "/js/**").permitAll()
        .requestMatchers("/livez", "/readyz", "/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
        .requestMatchers(STRIPE_WEBHOOK).permitAll()
        .requestMatchers("/api/user/notifications/**", "/user/notifications/**").denyAll()
        .requestMatchers("/api/auth/**", "/api/session", "/api/csrf", "/loginUser", "/logout",
            "/register/**", "/forgot-password", "/reset-password", "/api/user/profile/**",
            "/api/admin/users/**", "/api/admin/dashboard").denyAll()
        .requestMatchers(HttpMethod.GET, "/api/csrf", "/api/session", "/api/public/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .requestMatchers("/api/user/**", "/api/store/**", "/api/comments/**").hasRole("USER")
        .requestMatchers("/api/**").authenticated()
        .requestMatchers("/").permitAll()
        .requestMatchers(HttpMethod.POST, "/cars/*/comments", "/parts/*/comments").hasRole("USER")
        .requestMatchers(HttpMethod.GET,
            "/cars", "/cars/**",
            "/auctions", "/auctions/**", "/auction/**",
            "/live-auctions", "/browse-auctions", "/auction-listings").permitAll()
        .requestMatchers(HttpMethod.GET,
            "/listings", "/listings/**",
            "/car-listings", "/car-listings/**",
            "/cars-for-sale", "/cars-for-sale/**",
            "/vehicle-listings").permitAll()
        .requestMatchers(HttpMethod.GET,
            "/parts", "/parts/**",
            "/store", "/store/parts", "/store/parts/**",
            "/car-parts", "/car-parts/**",
            "/parts-store").permitAll()
        .requestMatchers(HttpMethod.POST, "/listings/**").hasRole("USER")
        .requestMatchers("/about-us", "/contact-us", "/view-user/**").permitAll()
        .requestMatchers("/register/**").permitAll()
        .requestMatchers("/forgot-password", "/reset-password").permitAll()

        .requestMatchers("/user/**").hasRole("USER")
        .requestMatchers("/listing-deposits/**").hasRole("USER")
        .requestMatchers("/car-bid/**").hasRole("USER")
        .requestMatchers("/test-drive/**").hasRole("USER")
        .requestMatchers("/cart/**", "/store/**", "/orders/**").hasRole("USER")

        .requestMatchers("/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated());

    var loginEntryPoint = new LoginUrlAuthenticationEntryPoint("/login");
    http.exceptionHandling(exceptions -> exceptions
        .authenticationEntryPoint((request, response, exception) -> {
          if (API_REQUEST.matches(request)) {
            writeApiError(response, HttpStatus.UNAUTHORIZED, "Authentication required.");
          } else {
            loginEntryPoint.commence(request, response, exception);
          }
        })
        .defaultAccessDeniedHandlerFor(
            (request, response, exception) -> {
              if (exception instanceof CsrfException) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Cache-Control", "no-store");
                response.getWriter().write("{\"code\":\"CSRF_INVALID\",\"message\":\"Your security token has expired. Refresh and try again.\",\"fieldErrors\":{}}");
              } else {
                writeApiError(response, HttpStatus.FORBIDDEN, "You do not have permission to perform this action.");
              }
            },
            API_REQUEST));

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedCorsOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN", "Idempotency-Key", "X-Request-ID"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }

  private void writeApiError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write("{\"message\":\"" + message + "\",\"fieldErrors\":{}}");
  }
}
