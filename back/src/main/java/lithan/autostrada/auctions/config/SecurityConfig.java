package lithan.autostrada.auctions.config;

import jakarta.servlet.DispatcherType;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public static PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
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
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers("/webhooks/stripe").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/session", "/api/public/**").permitAll()
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

    http.csrf(csrf -> csrf
        .ignoringRequestMatchers("/webhooks/stripe"));

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedCorsOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
