package lithan.autostrada.gateway;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component("upstream")
class UpstreamHealth implements ReactiveHealthIndicator {
  private final WebClient client = WebClient.create();
  private final String backend;
  private final String frontend;
  UpstreamHealth(@Value("${gateway.backend-url}") String backend, @Value("${gateway.frontend-url}") String frontend) {
    this.backend = backend; this.frontend = frontend;
  }
  private Mono<Boolean> available(String url) {
    return client.get().uri(url).exchangeToMono(response ->
        response.releaseBody().thenReturn(response.statusCode().is2xxSuccessful()))
        .timeout(Duration.ofSeconds(2)).onErrorReturn(false);
  }
  @Override public Mono<Health> health() {
    return Mono.zip(available(backend + "/actuator/health"), available(frontend + "/"))
        .map(states -> states.getT1() && states.getT2() ? Health.up().build() : Health.down().build());
  }
}
