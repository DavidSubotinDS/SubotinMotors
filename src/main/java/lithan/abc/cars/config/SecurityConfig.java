package lithan.abc.cars.config;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public static PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Autowired
  AuthenticationSuccessHandler successHandler;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    // Authorize
    http.authorizeHttpRequests(configurer -> configurer
        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
        .requestMatchers("/css/**", "/images/**", "/js/**").permitAll()
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers("/webhooks/stripe").permitAll()
        .requestMatchers("/").permitAll()
        .requestMatchers(HttpMethod.POST, "/cars/*/comments", "/parts/*/comments").hasRole("USER")
        .requestMatchers(HttpMethod.GET, "/listings", "/listings/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/listings/**").hasRole("USER")
        .requestMatchers("/cars/**").permitAll()
        .requestMatchers("/parts/**").permitAll()
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
}
