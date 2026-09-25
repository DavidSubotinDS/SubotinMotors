package lithan.autostrada.gateway;

import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("observability")
@AutoConfigureObservability
class MetricsBoundaryTests {
  static final int MANAGEMENT_PORT = freePort();
  @Autowired WebTestClient application;
  @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
    registry.add("management.server.port", () -> MANAGEMENT_PORT);
  }
  static int freePort() {
    try (ServerSocket socket = new ServerSocket(0)) { return socket.getLocalPort(); }
    catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
  }
  @Test void privateListenerExportsMetrics() {
    application.get().uri("/livez").exchange().expectStatus().isOk();
    WebTestClient.bindToServer().baseUrl("http://127.0.0.1:" + MANAGEMENT_PORT).build()
        .get().uri("/actuator/prometheus").exchange().expectStatus().isOk()
        .expectHeader().doesNotExist("Set-Cookie")
        .expectBody(String.class).value(body -> org.assertj.core.api.Assertions.assertThat(body).contains("jvm_memory_used_bytes"));
  }
  @Test void publicListenerRejectsMetricsAndSpoofedForwardingHeaders() {
    application.get().uri("/actuator/prometheus").header("X-Forwarded-Port", Integer.toString(MANAGEMENT_PORT))
        .exchange().expectStatus().isNotFound();
  }
  @Test void privateListenerRejectsMutations() {
    WebTestClient.bindToServer().baseUrl("http://127.0.0.1:" + MANAGEMENT_PORT).build()
        .post().uri("/actuator/prometheus").exchange().expectStatus().isNotFound();
  }
}
