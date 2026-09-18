package lithan.autostrada.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayTransportTests {
  record Received(String method, String uri, Map<String, String> headers, byte[] body) { }
  static final LinkedBlockingQueue<Received> requests = new LinkedBlockingQueue<>();
  static final DisposableServer backend = HttpServer.create().host("127.0.0.1").port(0)
      .handle((request, response) -> request.receive().aggregate().asByteArray().defaultIfEmpty(new byte[0]).flatMap(bytes -> {
        Map<String, String> headers = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        request.requestHeaders().forEach(entry -> headers.put(entry.getKey(), entry.getValue()));
        requests.add(new Received(request.method().name(), request.uri(), headers, bytes));
        if (request.uri().startsWith("/api/slow")) return Mono.delay(Duration.ofSeconds(3)).then(response.sendString(Mono.just("late")).then());
        if (request.uri().startsWith("/api/error")) return response.status(422).header("Content-Type", "application/json")
            .sendString(Mono.just("{\"message\":\"Invalid bid\",\"fieldErrors\":{\"bidPrice\":\"Too low\"}}")).then();
        if (request.uri().startsWith("/api/redirect")) return response.status(303)
            .header("Location", "http://localhost:8081/orders/7?paid=false#details")
            .addHeader("Set-Cookie", "JSESSIONID=opaque; Path=/; HttpOnly; SameSite=Lax")
            .addHeader("Set-Cookie", "preference=one; Path=/").send().then();
        if (request.uri().startsWith("/api/expired-cookie")) return response
            .header("Set-Cookie", "JSESSIONID=; Path=/; Max-Age=0; HttpOnly").send().then();
        if (request.uri().equals("/actuator/health")) return response.sendString(Mono.just("{\"status\":\"UP\"}")).then();
        return response.header("Content-Type", "application/octet-stream").sendByteArray(Mono.just(bytes)).then();
      })).bindNow();
  static final DisposableServer frontend = HttpServer.create().host("127.0.0.1").port(0)
      .handle((req, res) -> res.header("Content-Type", "text/html").sendString(Mono.just("<html>React assets</html>"))).bindNow();

  @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
    registry.add("gateway.backend-url", () -> "http://127.0.0.1:" + backend.port());
    registry.add("gateway.frontend-url", () -> "http://127.0.0.1:" + frontend.port());
    registry.add("spring.cloud.gateway.server.webflux.httpclient.response-timeout", () -> "1s");
  }
  @Autowired WebTestClient client;
  @org.springframework.boot.test.web.server.LocalServerPort int port;
  @BeforeEach void clear() { requests.clear(); }
  @AfterAll static void stop() { backend.disposeNow(); frontend.disposeNow(); }
  Received received() throws Exception {
    Received received = requests.poll(3, TimeUnit.SECONDS);
    assertThat(received).isNotNull();
    return received;
  }

  @ParameterizedTest @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
  void forwardsMethodsRawQueriesAndBodies(String method) throws Exception {
    byte[] body = "{ \"value\": \"a+b / č\" }\r\n".getBytes(StandardCharsets.UTF_8);
    String uri = "/api/public/items?q=a%2Bb&q=two+words&path=%2Fone%3Ftwo&empty=";
    client.method(HttpMethod.valueOf(method)).uri(java.net.URI.create("http://127.0.0.1:" + port + uri)).contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body).exchange().expectStatus().isOk().expectBody(byte[].class).isEqualTo(body);
    Received actual = received();
    assertThat(actual.method()).isEqualTo(method);
    assertThat(actual.uri()).isEqualTo(uri);
    assertThat(actual.body()).isEqualTo(body);
  }

  @Test void sessionCookiesAndRedirectsRemainBackendOwned() throws Exception {
    var result = client.get().uri("/api/redirect").header("Cookie", "JSESSIONID=opaque; preference=one")
        .exchange().expectStatus().isEqualTo(303)
        .expectHeader().valueEquals("Location", "http://localhost:8081/orders/7?paid=false#details")
        .expectBody().returnResult();
    assertThat(result.getResponseHeaders().get("Set-Cookie")).containsExactly(
        "JSESSIONID=opaque; Path=/; HttpOnly; SameSite=Lax", "preference=one; Path=/");
    assertThat(received().headers().get("Cookie")).isEqualTo("JSESSIONID=opaque; preference=one");
    client.get().uri("/api/expired-cookie").exchange().expectStatus().isOk()
        .expectHeader().valueEquals("Set-Cookie", "JSESSIONID=; Path=/; Max-Age=0; HttpOnly");
  }

  @Test void preservesRawSignedWebhookIncludingWhitespaceUtf8AndChunkBoundaries() throws Exception {
    byte[] body = "{\r\n  \"name\":\"Đorđe\", \"number\":1.00\r\n}\n".getBytes(StandardCharsets.UTF_8);
    var buffers = new org.springframework.core.io.buffer.DefaultDataBufferFactory();
    client.post().uri("/webhooks/stripe").header("Stripe-Signature", "t=123,v1=original")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Flux.just(buffers.wrap(java.util.Arrays.copyOfRange(body, 0, 9)),
            buffers.wrap(java.util.Arrays.copyOfRange(body, 9, body.length))), org.springframework.core.io.buffer.DataBuffer.class)
        .exchange().expectStatus().isOk().expectBody(byte[].class).isEqualTo(body);
    Received actual = received();
    assertThat(actual.body()).isEqualTo(body);
    assertThat(actual.headers().get("Stripe-Signature")).isEqualTo("t=123,v1=original");
  }

  @Test void multipartStreamsPastCodecBufferLimitWithoutRewriting() throws Exception {
    String boundary = "TransportFixtureBoundary";
    byte[] body = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"image.bin\"\r\n"
        + "Content-Type: application/octet-stream\r\n\r\n" + "x".repeat(1024 * 1024)
        + "\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
    client.mutate().codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)).build().post()
        .uri("/api/user/auctions").header("Content-Type", "multipart/form-data; boundary=" + boundary)
        .bodyValue(body).exchange().expectStatus().isOk().expectBody(byte[].class).isEqualTo(body);
    Received actual = received();
    assertThat(actual.headers().get("Content-Type")).isEqualTo("multipart/form-data; boundary=" + boundary);
    assertThat(actual.body()).isEqualTo(body);
  }

  @Test void backendErrorsAreNotSpaFallbacks() {
    client.get().uri("/api/error").exchange().expectStatus().isEqualTo(422)
        .expectHeader().contentType(MediaType.APPLICATION_JSON)
        .expectBody().json("{\"message\":\"Invalid bid\",\"fieldErrors\":{\"bidPrice\":\"Too low\"}}");
  }

  @Test void stripsForgedTrustHeadersAndConstructsConfiguredOrigin() throws Exception {
    var response = client.get().uri("/api/session")
        .header("Forwarded", "for=evil;host=evil;proto=https").header("X-Forwarded-For", "evil")
        .header("X-Forwarded-Host", "evil").header("X-Forwarded-Proto", "https")
        .header("X-Forwarded-Prefix", "/evil").header("X-User-Id", "1").header("X-Roles", "ROLE_ADMIN")
        .header("X-Internal-Token", "evil").header("Authorization", "Bearer evil")
        .header("X-E2E-Control", "evil").header("X-Request-ID", "forged")
        .exchange().expectStatus().isOk().expectBody().returnResult();
    var headers = received().headers();
    assertThat(headers).doesNotContainKeys("Forwarded", "X-Forwarded-Prefix", "X-User-Id", "X-Roles",
        "X-Internal-Token", "Authorization", "X-E2E-Control");
    assertThat(headers.get("Host")).isEqualTo("localhost:8081");
    assertThat(headers.get("X-Forwarded-Host")).isEqualTo("localhost:8081");
    assertThat(headers.get("X-Forwarded-Proto")).isEqualTo("http");
    assertThat(headers.get("X-Forwarded-For")).isEqualTo("127.0.0.1");
    assertThat(headers.get("X-Request-ID")).isEqualTo(response.getResponseHeaders().getFirst("X-Request-ID")).isNotEqualTo("forged");
  }

  @Test void corsHasOnePreciseCredentialCompatibleOwner() throws Exception {
    client.options().uri("/api/user/profile").header("Origin", "http://localhost:5173")
        .header("Access-Control-Request-Method", "PUT").header("Access-Control-Request-Headers", "Content-Type")
        .exchange().expectStatus().isOk().expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173")
        .expectHeader().valueEquals("Access-Control-Allow-Credentials", "true");
    assertThat(requests).isEmpty();
    client.get().uri("/api/session").header("Origin", "http://localhost:5173").exchange().expectStatus().isOk()
        .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:5173");
    assertThat(received().headers()).doesNotContainKey("Origin");
    client.options().uri("/api/user/profile").header("Origin", "https://evil.invalid")
        .header("Access-Control-Request-Method", "PUT").exchange().expectStatus().isForbidden()
        .expectHeader().doesNotExist("Access-Control-Allow-Origin");
    assertThat(requests).isEmpty();
  }

  @Test void canonicalPagesTerminateRedirectsAndLegacyActionsStillReachBackend() throws Exception {
    for (String path : List.of("/listings/7", "/parts/7", "/orders/7", "/store/checkout/success?session_id=7", "/user/auctions/7/edit"))
      client.get().uri(path).exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("<html>React assets</html>");
    assertThat(requests).isEmpty();
    for (String path : List.of("/car-listings/7", "/store/parts/7", "/cars?q=one", "/payments/seller/onboarding")) {
      client.get().uri(path).exchange().expectStatus().isOk();
      assertThat(received().uri()).isEqualTo(path);
    }
    for (String path : List.of("/user/listings/7", "/listings/7/test-rides", "/loginUser", "/logout", "/register/accountProcess")) {
      client.post().uri(path).contentType(MediaType.APPLICATION_FORM_URLENCODED).bodyValue("value=one%2Btwo")
          .exchange().expectStatus().isOk();
      assertThat(received().body()).isEqualTo("value=one%2Btwo".getBytes(StandardCharsets.UTF_8));
    }
  }

  @Test void internalControlsAndNonExactWebhookPathsAreNotPublic() {
    for (String path : List.of("/__e2e/reset", "/internal/v1/session-exchange", "/actuator/env",
        "/webhooks/stripe/extra", "/webhooks/stripe/"))
      client.post().uri(path).exchange().expectStatus().isNotFound();
    client.get().uri("/webhooks/stripe").exchange().expectStatus().isNotFound();
    assertThat(requests).isEmpty();
  }

  @Test void slowBackendHasBoundedJsonTimeout() {
    client.get().uri("/api/slow").exchange().expectStatus().isEqualTo(504)
        .expectHeader().contentType(MediaType.APPLICATION_JSON).expectBody().jsonPath("$.fieldErrors").isMap();
  }

}
