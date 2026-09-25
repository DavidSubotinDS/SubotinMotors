package lithan.autostrada.payment;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
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
        .authorizeHttpRequests(a->a
            .requestMatchers("/actuator/health","/actuator/health/liveness","/actuator/health/readiness").permitAll()
            .requestMatchers(HttpMethod.POST,"/webhooks/stripe").permitAll()
            .requestMatchers("/internal/v1/capabilities").hasAnyAuthority("SCOPE_payment-lookup","SCOPE_create-store-payment","SCOPE_create-deposit-payment")
            .requestMatchers(org.springframework.http.HttpMethod.POST,"/internal/v1/payments").hasAnyAuthority("SCOPE_create-store-payment","SCOPE_create-deposit-payment")
            .requestMatchers(org.springframework.http.HttpMethod.GET,"/internal/v1/payments/**","/internal/v1/payment-attempts/**","/internal/v1/provider-sessions/**").hasAuthority("SCOPE_payment-lookup")
            .requestMatchers(org.springframework.http.HttpMethod.POST,"/internal/v1/payments/*/expire").hasAuthority("SCOPE_payment-expire")
            .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/payments/**").authenticated()
            .anyRequest().denyAll())
        .exceptionHandling(e->e.authenticationEntryPoint((q,r,x)->error(r,401,"Authentication required."))
            .accessDeniedHandler((q,r,x)->error(r,403,"Payment purpose is not granted."))).build();
  }
  private void error(jakarta.servlet.http.HttpServletResponse r,int status,String message)throws java.io.IOException{
    r.setStatus(status);r.setContentType("application/json");r.setHeader("Cache-Control","no-store");
    r.getWriter().write("{\"message\":\""+message+"\",\"fieldErrors\":{}}");
  }
}
