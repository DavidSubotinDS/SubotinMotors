package lithan.autostrada.telemetry;

import lithan.autostrada.payment.MetricsSecurity;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

/** Real HTTP listeners; no database, session fixture or application peer required. */
@SpringBootTest(classes = MetricsBoundaryTests.Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("observability")
@AutoConfigureObservability
class MetricsBoundaryTests {
  static final int MANAGEMENT_PORT = freePort();
  @LocalServerPort int applicationPort;
  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration(excludeName = {
      "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
      "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
      "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"})
  @Import(MetricsSecurity.class)
  static class Application {}
  @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
    registry.add("management.server.port", () -> MANAGEMENT_PORT);
    registry.add("management.endpoint.health.group.readiness.include", () -> "readinessState");
  }
  static int freePort() {
    try (ServerSocket socket = new ServerSocket(0)) { return socket.getLocalPort(); }
    catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
  }
  HttpResponse<String> request(int port, String method) throws Exception {
    return HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/actuator/prometheus"))
        .timeout(java.time.Duration.ofSeconds(5)).header("X-Forwarded-Port", Integer.toString(MANAGEMENT_PORT))
        .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
  }
  @Test void privateListenerExportsMetricsWithoutCreatingSession() throws Exception {
    request(applicationPort, "GET");
    var response = request(MANAGEMENT_PORT, "GET");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("jvm_memory_used_bytes", "service=", "http_server_requests_seconds_bucket");
    assertThat(response.headers().allValues("set-cookie")).isEmpty();
  }
  @Test void applicationListenerRejectsMetricsEvenWithSpoofedForwardedPort() throws Exception {
    assertThat(request(applicationPort, "GET").statusCode()).isEqualTo(403);
  }
  @Test void metricsEndpointRejectsMutations() throws Exception {
    assertThat(request(MANAGEMENT_PORT, "POST").statusCode()).isEqualTo(403);
  }
}
