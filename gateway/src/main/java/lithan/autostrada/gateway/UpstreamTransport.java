package lithan.autostrada.gateway;

import java.time.Duration;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.*;
import reactor.netty.http.client.HttpClient;

/** Compose may replace an owner at a new address; do not retain Docker's long DNS TTL. */
@Configuration(proxyBeanMethods=false)
class UpstreamTransport {
  static HttpClient bounded(HttpClient client) {
    return client.disableRetry(true).resolver(r->r.cacheMinTimeToLive(Duration.ZERO)
        .cacheMaxTimeToLive(Duration.ofSeconds(5)).cacheNegativeTimeToLive(Duration.ZERO));
  }
  @Bean HttpClientCustomizer ownerAddressLifetime(){return UpstreamTransport::bounded;}
}
