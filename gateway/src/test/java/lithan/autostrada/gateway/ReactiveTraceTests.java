package lithan.autostrada.gateway;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.concurrent.*;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

/** Two exchanges must reach a blocked peer concurrently; production has no test route. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"management.tracing.enabled=true", "telemetry.log-path=target/reactive-requests.log"})
@ActiveProfiles("observability")
@AutoConfigureObservability
@Import(ReactiveTraceTests.Capture.class)
class ReactiveTraceTests {
  static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
  static final CountDownLatch ENTERED = new CountDownLatch(2);
  static final CountDownLatch RELEASE = new CountDownLatch(1);
  static final ConcurrentLinkedQueue<String> HEADERS = new ConcurrentLinkedQueue<>();
  static final ConcurrentLinkedQueue<SpanData> SPANS = new ConcurrentLinkedQueue<>();
  static final HttpServer PEER = peer();
  static final int MANAGEMENT_PORT = MetricsBoundaryTests.freePort();
  static final String TRACE_ID = "12345678901234567890123456789012";
  @LocalServerPort int port;
  @Autowired SdkTracerProvider tracerProvider;
  @Autowired io.micrometer.core.instrument.MeterRegistry meters;

  static HttpServer peer() {
    try {
      var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.setExecutor(EXECUTOR);
      server.createContext("/", exchange -> {
        boolean identity = exchange.getRequestURI().getPath().equals("/internal/v1/session-exchange");
        String parent = exchange.getRequestHeaders().getFirst("traceparent");
        if (parent != null) HEADERS.add(parent);
        if (identity) {
          ENTERED.countDown();
          try { RELEASE.await(5, TimeUnit.SECONDS); }
          catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        }
        exchange.getRequestBody().readAllBytes();
        byte[] response = (identity ? "{\"assertion\":\"fixture\"}" : "{}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
      });
      server.start();
      return server;
    } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
  }
  @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
    String peer = "http://127.0.0.1:" + PEER.getAddress().getPort();
    for (String key : java.util.List.of("gateway.identity-url", "gateway.backend-url", "gateway.frontend-url", "gateway.payment-url")) {
      registry.add(key, () -> peer);
    }
    registry.add("gateway.identity-secret", () -> "fixture-private-gateway-secret-32bytes");
    registry.add("management.tracing.sampling.probability", () -> "1.0");
    registry.add("spring.reactor.context-propagation", () -> "auto");
    registry.add("management.server.port", () -> MANAGEMENT_PORT);
    registry.add("telemetry.trace-url", () -> peer + "/v1/traces");
  }
  @TestConfiguration(proxyBeanMethods = false)
  static class Capture {
    @Bean SpanExporter captureExporter() {
      return new SpanExporter() {
        public CompletableResultCode export(Collection<SpanData> spans) { SPANS.addAll(spans); return CompletableResultCode.ofSuccess(); }
        public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
        public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
      };
    }
  }
  @Test void concurrentExchangesPropagateChildSpansWithoutReplayingRequests() throws Exception {
    var logPath = java.nio.file.Path.of("target/reactive-requests.log");
    long priorLogSize = java.nio.file.Files.exists(logPath) ? java.nio.file.Files.size(logPath) : 0;
    var client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build();
    var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/user/telemetry-fixture?password=secret-sentinel"))
        .timeout(java.time.Duration.ofSeconds(8)).header("Cookie", "AUTOSTRADA_SESSION=fixture")
        .header("traceparent", "00-" + TRACE_ID + "-1234567890123456-01").build();
    var first = client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    var second = client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    try { assertThat(ENTERED.await(1, TimeUnit.SECONDS)).as("Both exchanges reached the peer before either response").isTrue(); }
    finally { RELEASE.countDown(); }
    assertThat(first.get(8, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
    assertThat(second.get(8, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
    tracerProvider.forceFlush().join(5, TimeUnit.SECONDS);
    assertThat(HEADERS).hasSize(4).allSatisfy(value -> assertThat(value).startsWith("00-" + TRACE_ID + "-"));
    assertThat(HEADERS.stream().distinct().count()).isEqualTo(4);
    assertThat(SPANS).anySatisfy(span -> {
      assertThat(span.getTraceId()).isEqualTo(TRACE_ID);
      assertThat(span.getKind()).isEqualTo(io.opentelemetry.api.trace.SpanKind.CLIENT);
    });
    assertThat(meters.find("gateway.http.client.requests").timer()).isNotNull();
    String requestLogs = java.nio.file.Files.readString(logPath).substring((int) priorLogSize);
    assertThat(requestLogs).contains("trace_id=" + TRACE_ID).doesNotContain("secret-sentinel", "telemetry-fixture", "AUTOSTRADA_SESSION");
  }
  @AfterAll static void stop() { RELEASE.countDown(); PEER.stop(0); EXECUTOR.shutdownNow(); }
}
