package lithan.autostrada.gateway;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.*;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.*;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.*;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/** Per-request session exchange. Browser credentials never reach business owners. */
@Component
class IdentityBridge implements GlobalFilter, Ordered {
  private final WebClient identity;
  private final String secret;
  IdentityBridge(@Value("${gateway.identity-url:http://127.0.0.1:8082}") String url,
      @Value("${gateway.identity-secret:}") String secret, WebClient.Builder builder) {
    this.secret=secret;
    var pool=ConnectionProvider.builder("identity-exchange").maxConnections(32).pendingAcquireMaxCount(64)
        .pendingAcquireTimeout(Duration.ofMillis(300)).maxIdleTime(Duration.ofSeconds(5)).maxLifeTime(Duration.ofSeconds(5)).build();
    var client=UpstreamTransport.bounded(HttpClient.create(pool)).option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS,300)
        .responseTimeout(Duration.ofSeconds(2));
    identity=builder.clone().baseUrl(url).clientConnector(new ReactorClientHttpConnector(client))
        .codecs(c->c.defaultCodecs().maxInMemorySize(65536)).build();
  }
  @Override public int getOrder(){return -10;}
  @Override public Mono<Void> filter(ServerWebExchange exchange,GatewayFilterChain chain) {
    Route route=exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    if(route==null || !Set.of("api","legacy","notification-api","notification-legacy","payment-api").contains(route.getId())) return chain.filter(exchange);
    var request=exchange.getRequest();
    boolean read=Set.of(HttpMethod.GET,HttpMethod.HEAD,HttpMethod.OPTIONS).contains(request.getMethod());
    String path=request.getPath().value();
    boolean publicRead=read && (path.startsWith("/api/public/") || publicLegacy(path));
    var cookie=request.getCookies().getFirst("AUTOSTRADA_SESSION");
    if(publicRead && cookie==null) return chain.filter(withAssertion(exchange,null));
    // A valid session may personalize public data (for example, followed
    // auctions). An expired cookie degrades to the same anonymous response as
    // no cookie instead of making public data unavailable.
    if(publicRead) return exchange(exchange,chain,null,true);
    if(!read && MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(request.getHeaders().getContentType())
        && request.getHeaders().getFirst("X-CSRF-TOKEN")==null) {
      // Legacy URL-encoded forms may put _csrf in the body. Replay these exact bounded bytes once.
      // Multipart and signed webhook bodies are never aggregated or decoded here.
      return DataBufferUtils.join(request.getBody(),1024*1024).defaultIfEmpty(exchange.getResponse().bufferFactory().wrap(new byte[0]))
          .flatMap(buffer->{
            byte[] bytes=new byte[buffer.readableByteCount()];buffer.read(bytes);DataBufferUtils.release(buffer);
            String csrf=null;
            for(String part:new String(bytes,StandardCharsets.UTF_8).split("&")) {
              String[] pair=part.split("=",2);
              if(URLDecoder.decode(pair[0],StandardCharsets.UTF_8).equals("_csrf") && pair.length==2)
                csrf=URLDecoder.decode(pair[1],StandardCharsets.UTF_8);
            }
            var replay=new ServerHttpRequestDecorator(request) {
              @Override public Flux<DataBuffer> getBody(){return Flux.defer(()->Flux.just(exchange.getResponse().bufferFactory().wrap(bytes)));}
            };
            return exchange(exchange.mutate().request(replay).build(),chain,csrf,false);
          });
    }
    return exchange(exchange,chain,request.getHeaders().getFirst("X-CSRF-TOKEN"),false);
  }
  private Mono<Void> exchange(ServerWebExchange exchange,GatewayFilterChain chain,String csrf,
      boolean anonymousOnUnauthorized) {
    if(secret.length()<32) return EdgeBoundary.error(exchange,HttpStatus.SERVICE_UNAVAILABLE);
    var cookie=exchange.getRequest().getCookies().getFirst("AUTOSTRADA_SESSION");
    Route route=exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    Map<String,String> body=new HashMap<>();body.put("audience",route.getId().startsWith("notification-") ? "notification-service" : route.getId().equals("payment-api") ? "payment-service" : "legacy-backend");
    body.put("method",exchange.getRequest().getMethod().name());if(csrf!=null)body.put("csrfToken",csrf);
    return identity.post().uri("/internal/v1/session-exchange").headers(h->{
      h.setBasicAuth("gateway",secret);
      if(cookie!=null)h.set(HttpHeaders.COOKIE,"AUTOSTRADA_SESSION="+cookie.getValue());
      for(String name:List.of("X-Request-ID")) {
        String value=exchange.getRequest().getHeaders().getFirst(name);if(value!=null)h.set(name,value);
      }
    }).bodyValue(body).exchangeToMono(response -> response.statusCode().is2xxSuccessful()
        ? response.bodyToMono(Assertion.class).map(a->new Result(200,a.assertion(),null))
        : response.bodyToMono(byte[].class).defaultIfEmpty(new byte[0]).map(bytes->new Result(response.statusCode().value(),null,bytes)))
      .timeout(Duration.ofSeconds(3)).onErrorReturn(new Result(503,null,null)).flatMap(result -> {
      if(result.status()==200) return chain.filter(withAssertion(exchange,result.assertion()));
      if(result.status()==403) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(result.error())));
      }
      if(result.status()==401 && anonymousOnUnauthorized) return chain.filter(withAssertion(exchange,null));
      if(result.status()==401) return authenticationRequired(exchange);
      return EdgeBoundary.error(exchange,HttpStatus.SERVICE_UNAVAILABLE);
    });
  }
  private Mono<Void> authenticationRequired(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    exchange.getResponse().getHeaders().setCacheControl("no-store");
    byte[] body="{\"message\":\"Authentication required.\",\"fieldErrors\":{}}"
        .getBytes(StandardCharsets.UTF_8);
    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
  }
  private record Result(int status,String assertion,byte[] error) { @Override public String toString(){return "Result[redacted]";} }
  private record Assertion(String assertion) { @Override public String toString(){return "Assertion[redacted]";} }
  private ServerWebExchange withAssertion(ServerWebExchange exchange,String token) {
    var original=exchange.getRequest();
    var mutated=original.mutate().headers(h->{
      h.remove(HttpHeaders.COOKIE);h.remove(HttpHeaders.AUTHORIZATION);h.remove("X-CSRF-TOKEN");
      if(token!=null)h.setBearerAuth(token);
    }).build();
    var request=new ServerHttpRequestDecorator(mutated) {
      @Override public Flux<DataBuffer> getBody(){return original.getBody();}
    };
    return exchange.mutate().request(request).build();
  }
  private boolean publicLegacy(String p) {
    return p.equals("/cars") || p.startsWith("/cars/") || p.startsWith("/auctions/")
        || p.startsWith("/auction/") || p.startsWith("/listings/") || p.startsWith("/car-listings")
        || p.startsWith("/cars-for-sale") || p.startsWith("/parts/") || p.startsWith("/store/parts")
        || p.startsWith("/car-parts") || p.startsWith("/view-user/")
        || Set.of("/live-auctions","/browse-auctions","/auction-listings","/vehicle-listings","/store","/parts-store").contains(p);
  }
}
