package lithan.autostrada.auctions.identity;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class RemoteProfileClientTests {
  HttpServer server;
  RemoteProfileClient client;
  List<String> reads=new CopyOnWriteArrayList<>();
  int status=200;
  @BeforeEach void start() throws Exception {
    server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
    server.createContext("/internal/v1/", exchange -> {
      String path=exchange.getRequestURI().toString();
      String body;
      int response=status;
      if(path.endsWith("service-token")) {
        response=200;
        body="{\"accessToken\":\"test-only-grant\",\"expiresIn\":60}";
      } else {
        reads.add(path);
        if(!"Bearer test-only-grant".equals(exchange.getRequestHeaders().getFirst("Authorization"))) response=401;
        body=path.contains("checkout-profile") ? "{\"userId\":7,\"email\":\"buyer@example.test\",\"name\":\"Buyer\"}"
            : "{\"7\":{\"userId\":7,\"profileId\":19,\"username\":\"buyer\",\"adminBadge\":false}}";
      }
      byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type","application/json");
      exchange.sendResponseHeaders(response,bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.start();
    client=new RemoteProfileClient("http://127.0.0.1:"+server.getAddress().getPort(),"test-only-secret",()->7,org.springframework.web.client.RestClient.builder());
  }
  @AfterEach void stop() {server.stop(0);}
  @Test void readsAndMapsProfilesAcrossRealHttp() {
    var profile=client.findAll(List.of(7,7)).get(7);
    assertThat(profile.userId()).isEqualTo(7);
    assertThat(profile.profileId()).isEqualTo(19);
    assertThat(reads).containsExactly("/internal/v1/profiles?ids=7");
  }
  @Test void batchesWithoutOneCallPerAccount() {
    client.findAll(IntStream.rangeClosed(1,205).boxed().toList());
    assertThat(reads).hasSize(3);
    assertThat(reads).allMatch(path->path.substring(path.indexOf('=')+1).split(",").length<=100);
  }
  @Test void emptyDisplayListNeedsNoPeer() {
    assertThat(client.findAll(List.of())).isEmpty();
    assertThat(reads).isEmpty();
  }
  @Test void privateLookupUsesAuthenticatedActor() {
    assertThat(client.current().userId()).isEqualTo(7);
    assertThat(reads).containsExactly("/internal/v1/users/7/checkout-profile");
  }
  @Test void dependencyErrorsAreControlledWithoutReplay() {
    status=503;
    assertThatThrownBy(()->client.findAll(List.of(7))).isInstanceOf(IdentityUnavailableException.class);
    assertThat(reads).hasSize(1);
  }
}
