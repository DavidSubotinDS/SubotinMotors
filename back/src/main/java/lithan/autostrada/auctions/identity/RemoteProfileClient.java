package lithan.autostrada.auctions.identity;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import lithan.autostrada.auctions.dto.api.ApiModels.ProfileResponse;

/** Minimal bounded client. No retries, display cache or caller-selected private account IDs. */
@Service
public class RemoteProfileClient implements ProfileClient, CheckoutProfileClient {
  private static final Logger log=LoggerFactory.getLogger(RemoteProfileClient.class);
  private final RestClient http; private final String secret; private final CurrentIdentity actor;
  private final Semaphore capacity=new Semaphore(32);
  public RemoteProfileClient(@Value("${identity.base-url}") String url,
      @Value("${identity.service-secret}") String secret, CurrentIdentity actor) {
    var transport=new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder()
        .connectTimeout(Duration.ofMillis(300)).followRedirects(java.net.http.HttpClient.Redirect.NEVER).build());
    transport.setReadTimeout(Duration.ofSeconds(2));
    this.http=RestClient.builder().baseUrl(url).requestFactory(transport).build(); this.secret=secret; this.actor=actor;
  }
  public record Token(String accessToken,int expiresIn) { @Override public String toString(){return "Token[redacted]";} }
  private String token() {
    return http.post().uri("/internal/v1/service-token").headers(h->h.setBasicAuth("legacy-backend",secret))
        .body(Map.of("audience","identity-service")).retrieve().body(Token.class).accessToken();
  }
  private <T> T request(String path,ParameterizedTypeReference<T> type,boolean missingAllowed) {
    if(!capacity.tryAcquire()) throw new IdentityUnavailableException();
    try {
      return get(path,type);
    } catch(org.springframework.web.client.HttpClientErrorException.NotFound ex) {
      if(missingAllowed) return null; throw new IdentityUnavailableException();
    } catch(RuntimeException ex) {
      Throwable cause=ex.getCause();
      log.warn("Identity profile request failed: exception_type={} cause_type={}",
          ex.getClass().getName(),cause==null ? "none" : cause.getClass().getName());
      throw new IdentityUnavailableException();
    }
    finally {capacity.release();}
  }
  private <T> T get(String path,ParameterizedTypeReference<T> type) {
    return http.get().uri(path).headers(h->{h.setBearerAuth(token());correlate(h);}).retrieve().body(type);
  }
  private void correlate(HttpHeaders headers) {
    var attrs=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
    if(attrs instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
      for(String key:List.of("X-Request-ID","traceparent","tracestate")) {
        String value=servlet.getRequest().getHeader(key);if(value!=null) headers.set(key,value);
      }
    }
  }
  @Override public Map<Integer,PublicProfile> findAll(Collection<Integer> values) {
    var ids=new ArrayList<>(new LinkedHashSet<>(values));ids.removeIf(id->id==null || id<=0);
    if(ids.isEmpty()) return Map.of();
    var result=new LinkedHashMap<Integer,PublicProfile>();
    for(int start=0;start<ids.size();start+=100) {
      String query=ids.subList(start,Math.min(start+100,ids.size())).stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
      Map<Integer,PublicProfile> batch=request("/internal/v1/profiles?ids="+query,new ParameterizedTypeReference<>(){},false);
      result.putAll(batch);
    }
    return Map.copyOf(result);
  }
  @Override public Optional<PublicProfile> findByProfileId(int id) {
    return Optional.ofNullable(request("/internal/v1/profiles/"+id,new ParameterizedTypeReference<PublicProfile>(){},true));
  }
  @Override public CheckoutProfile current() {
    return request("/internal/v1/users/"+actor.requireUserId()+"/checkout-profile",new ParameterizedTypeReference<>(){},false);
  }
  public ProfileResponse self() {
    return request("/internal/v1/users/"+actor.requireUserId()+"/self-profile",new ParameterizedTypeReference<>(){},false);
  }
}
