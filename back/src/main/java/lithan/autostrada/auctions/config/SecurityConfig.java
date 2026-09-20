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

  @Bean
  public HttpSessionCsrfTokenRepository csrfTokenRepository() {
    return new HttpSessionCsrfTokenRepository();
  }

  // Controller authentication must explicitly invoke the same framework lifecycle
  // that form login invokes in its authentication filter.
  @Bean
  public SessionAuthenticationStrategy apiSessionAuthenticationStrategy(HttpSessionCsrfTokenRepository repository) {
    var fixation = new ChangeSessionIdAuthenticationStrategy();
    var csrf = new CsrfAuthenticationStrategy(repository);
    return (authentication, request, response) -> {
      fixation.onAuthentication(authentication, request, response);
      csrf.onAuthentication(authentication, request, response);
    };
  }

  @Bean
  public static PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Autowired
  AuthenticationSuccessHandler successHandler;

  @Value("${app.cors.allowed-origins:http://localhost:5173}")
  private List<String> allowedCorsOrigins;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http.cors(cors -> { });

    // Authorize
    http.authorizeHttpRequests(configurer -> configurer
        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
        .requestMatchers("/css/**", "/images/**", "/js/**").permitAll()
        .requestMatchers("/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness").permitAll()
        .requestMatchers(STRIPE_WEBHOOK).permitAll()
        .requestMatchers("/api/auth/**").permitAll()
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

    // Form Login
    http.formLogin(form -> form
        .loginPage("/login")
        .loginProcessingUrl("/loginUser")
        .successHandler(successHandler)
        .permitAll());

    // Logout
    http.logout(logout -> logout
        .logoutUrl("/logout")
        .permitAll());

    http.sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()));
    http.csrf(csrf -> csrf
        .csrfTokenRepository(csrfTokenRepository())
        .ignoringRequestMatchers(STRIPE_WEBHOOK));

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
