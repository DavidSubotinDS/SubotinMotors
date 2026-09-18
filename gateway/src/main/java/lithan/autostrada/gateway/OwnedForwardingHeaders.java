package lithan.autostrada.gateway;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

/** Runs after SCG's removal filters; only the configured edge can produce these values. */
@Component
class OwnedForwardingHeaders implements HttpHeadersFilter, Ordered {
  private final URI origin;
  OwnedForwardingHeaders(@Value("${gateway.public-url}") URI origin) { this.origin = origin; }
  @Override public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
  @Override public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
    HttpHeaders headers = new HttpHeaders();
    headers.addAll(input);
    headers.set("X-Forwarded-Host", origin.getRawAuthority());
    headers.set("X-Forwarded-Proto", origin.getScheme());
    headers.set("X-Forwarded-Port", String.valueOf(origin.getPort() >= 0 ? origin.getPort()
        : origin.getScheme().equals("https") ? 443 : 80));
    var remote = exchange.getRequest().getRemoteAddress();
    if (remote != null && remote.getAddress() != null)
      headers.set("X-Forwarded-For", remote.getAddress().getHostAddress());
    return headers;
  }
}
