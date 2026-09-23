package lithan.autostrada.notification;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {
  @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http.csrf(c->c.disable()).cors(c->c.disable()).requestCache(c->c.disable())
        .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(c->c.disable()).httpBasic(c->c.disable()).logout(c->c.disable())
        .oauth2ResourceServer(o->o.jwt(j->j.jwtAuthenticationConverter(AssertionSecurity.converter())))
        .authorizeHttpRequests(a->a.requestMatchers("/actuator/health","/actuator/health/liveness","/actuator/health/readiness").permitAll()
            .requestMatchers("/internal/v1/users/*/unread-count").hasAuthority("SCOPE_notification-count")
            .requestMatchers("/api/user/notifications/**","/user/notifications/**").hasRole("USER").anyRequest().denyAll())
        .exceptionHandling(e->e.authenticationEntryPoint((q,r,x)->error(r,401,"Authentication required."))
            .accessDeniedHandler((q,r,x)->error(r,403,"You do not have permission to perform this action.")))
        .build();
  }
  private void error(jakarta.servlet.http.HttpServletResponse response,int status,String message) throws java.io.IOException {
    response.setStatus(status);response.setContentType("application/json");response.setHeader("Cache-Control","no-store");
    response.getWriter().write("{\"message\":\""+message+"\",\"fieldErrors\":{}}");
  }
}
