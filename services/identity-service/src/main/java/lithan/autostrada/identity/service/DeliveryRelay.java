package lithan.autostrada.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="notification.relay.enabled",havingValue="true",matchIfMissing=true)
public class DeliveryRelay {
  private final JdbcTemplate db;private final ObjectMapper json;private final Clock clock;
  private final ConnectionFactory factory=new ConnectionFactory();
  public DeliveryRelay(JdbcTemplate db,ObjectMapper json,Clock clock,@Value("${notification.broker.host:localhost}") String host,
      @Value("${notification.broker.port:5672}") int port,@Value("${notification.broker.username:identity}") String user,
      @Value("${notification.broker.password:}") String password,@Value("${notification.broker.vhost:autostrada}") String vhost) {
    this.db=db;this.json=json;this.clock=clock;factory.setHost(host);factory.setPort(port);factory.setUsername(user);
    factory.setPassword(password);factory.setVirtualHost(vhost);factory.setAutomaticRecoveryEnabled(false);
    factory.setConnectionTimeout(1000);factory.setHandshakeTimeout(2000);factory.setChannelRpcTimeout(2000);
  }
  @Scheduled(fixedDelayString="${notification.relay.poll-ms:1000}")
  public void relay() {
    Instant now=clock.instant();
    db.update("UPDATE tb_delivery_outbox SET encrypted_payload=NULL WHERE expires_at<=?",Timestamp.from(now));
    var rows=db.queryForList("SELECT * FROM tb_delivery_outbox WHERE published_at IS NULL AND encrypted_payload IS NOT NULL AND next_attempt_at<=? ORDER BY created_at LIMIT 20",Timestamp.from(now));
    for(var row:rows) {
      String id=(String)row.get("event_id");Instant expiry=((Timestamp)row.get("expires_at")).toInstant();
      try(var connection=factory.newConnection("identity-delivery");var channel=connection.createChannel()) {
        Map<String,Object> event=new LinkedHashMap<>();event.put("eventId",id);event.put("eventType","identity.password-reset-delivery.v1");
        event.put("schemaVersion",1);event.put("occurredAt",((Timestamp)row.get("created_at")).toInstant().toString());
        event.put("producer","identity-service");event.put("aggregateType","PasswordResetDelivery");event.put("aggregateId",id);
        event.put("aggregateVersion",1);event.put("correlationId",id);event.put("causationId",null);
        event.put("payload",Map.of("deliveryId",id,"expiresAt",expiry.toString(),"encryptedPayload",row.get("encrypted_payload")));
        long ttl=Duration.between(clock.instant(),expiry).toMillis();if(ttl<=0)continue;
        AtomicBoolean returned=new AtomicBoolean();channel.addReturnListener((code,text,exchange,routing,props,body)->returned.set(true));
        channel.confirmSelect();
        channel.basicPublish("autostrada.commands","identity.password-reset-delivery.v1",true,
            new AMQP.BasicProperties.Builder().contentType("application/json").deliveryMode(2).messageId(id).correlationId(id).expiration(Long.toString(ttl)).build(),json.writeValueAsBytes(event));
        channel.waitForConfirmsOrDie(2000);if(returned.get())throw new IllegalStateException("Unroutable delivery");
        db.update("UPDATE tb_delivery_outbox SET published_at=?,encrypted_payload=NULL WHERE event_id=?",Timestamp.from(clock.instant()),id);
      }catch(Exception ignored) {
        int attempts=((Number)row.get("attempts")).intValue();long delay=Math.min(120,5L*(1L<<Math.min(attempts,5)));
        db.update("UPDATE tb_delivery_outbox SET attempts=attempts+1,next_attempt_at=? WHERE event_id=?",Timestamp.from(clock.instant().plusSeconds(delay)),id);
      }
    }
  }
}
