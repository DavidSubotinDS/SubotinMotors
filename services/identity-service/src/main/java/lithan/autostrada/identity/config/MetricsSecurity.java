package lithan.autostrada.identity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/** Metrics have no account authority and are served only on the private listener. */
@Configuration(proxyBeanMethods = false)
@Profile("observability")
public class MetricsSecurity {
  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  SecurityFilterChain metricsChain(HttpSecurity http,
      @Value("${management.server.port}") int port) throws Exception {
    if (port <= 0) throw new IllegalArgumentException("An explicit positive management port is required");
    return http.securityMatcher(request -> request.getRequestURI().equals("/actuator/prometheus"))
        .csrf(c -> c.disable()).requestCache(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .requestMatchers(request -> request.getLocalPort() == port && request.getMethod().equals("GET")).permitAll()
            .anyRequest().denyAll())
        .exceptionHandling(e -> e.authenticationEntryPoint((q, r, x) -> r.setStatus(403)))
        .build();
  }
}
