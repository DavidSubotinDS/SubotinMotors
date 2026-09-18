package lithan.autostrada.auctions.config;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Correlates private backend requests with edge logs without logging URLs, bodies or credentials. */
@Component
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnProperty(name = "app.gateway-request-logging", havingValue = "true")
public class GatewayRequestLoggingFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(GatewayRequestLoggingFilter.class);
  @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {
    String id = request.getHeader("X-Request-ID");
    if (id == null || !id.matches("[a-f0-9-]{36}")) id = UUID.randomUUID().toString();
    long started = System.nanoTime();
    try { chain.doFilter(request, response); }
    finally {
      log.info("service=backend request_id={} method={} status={} duration_ms={}", id,
          request.getMethod(), response.getStatus(), (System.nanoTime() - started) / 1_000_000);
    }
  }
}
