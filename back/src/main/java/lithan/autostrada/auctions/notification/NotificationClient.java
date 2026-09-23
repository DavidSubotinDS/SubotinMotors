package lithan.autostrada.auctions.notification;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import lithan.autostrada.auctions.identity.CurrentIdentity;

@Service
public class NotificationClient {
  private final RestClient identity,notification;
  private final String secret;
  private final CurrentIdentity actor;
  private final Semaphore capacity=new Semaphore(16);
  public NotificationClient(@Value("${identity.base-url}") String identityUrl,
      @Value("${notification.base-url:http://127.0.0.1:8083}") String notificationUrl,
      @Value("${identity.service-secret}") String secret,CurrentIdentity actor) {
    var transport=new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofMillis(300))
        .followRedirects(java.net.http.HttpClient.Redirect.NEVER).build());transport.setReadTimeout(Duration.ofSeconds(2));
    identity=RestClient.builder().baseUrl(identityUrl).requestFactory(transport).build();
    notification=RestClient.builder().baseUrl(notificationUrl).requestFactory(transport).build();this.secret=secret;this.actor=actor;
  }
  private record Token(String accessToken) { @Override public String toString(){return "Token[redacted]";} }
  private record Count(long count) {}
  public long unreadCount() {
    int user=actor.requireUserId();
    if(!capacity.tryAcquire())throw unavailable();
    try {
      var token=identity.post().uri("/internal/v1/service-token").headers(h->h.setBasicAuth("legacy-backend",secret))
          .body(Map.of("audience","notification-service")).retrieve().body(Token.class);
      return notification.get().uri("/internal/v1/users/"+user+"/unread-count").headers(h->h.setBearerAuth(token.accessToken()))
          .retrieve().body(Count.class).count();
    }catch(RuntimeException ignored){throw unavailable();}finally{capacity.release();}
  }
  private NotificationUnavailableException unavailable(){return new NotificationUnavailableException();}
}
