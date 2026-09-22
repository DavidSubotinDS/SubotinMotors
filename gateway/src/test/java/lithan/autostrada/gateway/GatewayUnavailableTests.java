package lithan.autostrada.gateway;

import java.net.ServerSocket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/** Startup must succeed without either upstream. No database or other executable participates. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayUnavailableTests {
  static final int closedPort = closedPort();
  static int closedPort() {
    try (ServerSocket socket = new ServerSocket(0)) { return socket.getLocalPort(); }
    catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
  }
  @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
    registry.add("gateway.backend-url", () -> "http://127.0.0.1:" + closedPort);
    registry.add("gateway.identity-url", () -> "http://127.0.0.1:" + closedPort);
    registry.add("gateway.identity-secret", () -> "unavailable-fixture-secret-32-bytes");
    registry.add("gateway.frontend-url", () -> "http://127.0.0.1:" + closedPort);
  }
  @Autowired WebTestClient client;
  @Test void unavailableBackendReturnsBoundedJsonError() {
    client.get().uri("/api/session").exchange().expectStatus().isEqualTo(502)
        .expectBody().jsonPath("$.message").isEqualTo("Request could not be completed.");
  }
  @Test void livenessSurvivesButReadinessReflectsMissingUpstreams() {
    client.get().uri("/actuator/health/liveness").exchange().expectStatus().isOk();
    client.get().uri("/actuator/health/readiness").exchange().expectStatus().isEqualTo(503);
  }
}
