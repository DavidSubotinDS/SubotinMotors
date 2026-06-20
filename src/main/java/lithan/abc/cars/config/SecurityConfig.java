package lithan.abc.cars.config;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        .requestMatchers("/").permitAll()
        .requestMatchers("/cars/**").permitAll()
        .requestMatchers("/about-us", "/contact-us", "/view-user/**").permitAll()
        .requestMatchers("/register/**").permitAll()

        .requestMatchers("/user/**").hasRole("USER")
        .requestMatchers("/car-bid/**").hasRole("USER")
        .requestMatchers("/test-drive/**").hasRole("USER")

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

    return http.build();
  }
}
