package lithan.autostrada.auctions.notification;

import com.rabbitmq.client.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** At-least-once relay: broker confirmation and routing precede the published marker. */
@Component
@ConditionalOnProperty(name="notification.relay.enabled",havingValue="true",matchIfMissing=true)
class OutboxRelay {
  private final JdbcTemplate db;
  private final java.time.Clock clock;
  private final ConnectionFactory factory=new ConnectionFactory();
  private final MeterRegistry metrics;
  private final java.util.concurrent.atomic.AtomicLong oldestSeconds=new java.util.concurrent.atomic.AtomicLong();
  OutboxRelay(JdbcTemplate db,MeterRegistry metrics,java.time.Clock clock,@Value("${notification.broker.host:localhost}") String host,
      @Value("${notification.broker.port:5672}") int port,@Value("${notification.broker.username:backend}") String user,
      @Value("${notification.broker.password:}") String password,@Value("${notification.broker.vhost:autostrada}") String vhost) {
    this.db=db;this.metrics=metrics;this.clock=clock;factory.setHost(host);factory.setPort(port);factory.setUsername(user);
    metrics.gauge("notification.outbox.oldest.seconds",oldestSeconds);
    factory.setPassword(password);factory.setVirtualHost(vhost);factory.setAutomaticRecoveryEnabled(false);
    factory.setConnectionTimeout(1000);factory.setHandshakeTimeout(2000);factory.setChannelRpcTimeout(2000);
  }
  @Scheduled(fixedDelayString="${notification.relay.poll-ms:1000}")
  public void relay() {
    List<Map<String,Object>> rows;
    try {
      var oldest=db.queryForObject("SELECT MIN(created_at) FROM tb_notification_outbox WHERE published_at IS NULL",Timestamp.class);
      oldestSeconds.set(oldest==null?0:Math.max(0,java.time.Duration.between(oldest.toInstant(),clock.instant()).toSeconds()));
      rows=db.queryForList("SELECT event_id,payload,attempts FROM tb_notification_outbox WHERE published_at IS NULL AND next_attempt_at<=? ORDER BY created_at LIMIT 20",Timestamp.from(clock.instant()));}
    catch(RuntimeException ignored){metrics.counter("notification.outbox.database.errors").increment();return;}
    if(rows.isEmpty())return;
    for(var row:rows) {
      String id=(String)row.get("event_id");
      try(var connection=factory.newConnection("backend-outbox");var channel=connection.createChannel()) {
        AtomicBoolean returned=new AtomicBoolean();channel.addReturnListener((code,text,exchange,routing,props,body)->returned.set(true));
        channel.confirmSelect();
        var envelope=new com.fasterxml.jackson.databind.ObjectMapper().readTree((String)row.get("payload"));
        var headers=new HashMap<String,Object>();if(envelope.has("traceparent"))headers.put("traceparent",envelope.path("traceparent").asText());
        channel.basicPublish("autostrada.events","marketplace.auction-ending-soon.v1",true,
            new AMQP.BasicProperties.Builder().contentType("application/json").deliveryMode(2).messageId(id).correlationId(envelope.path("correlationId").asText()).headers(headers).build(),
            ((String)row.get("payload")).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        channel.waitForConfirmsOrDie(2000);if(returned.get())throw new IllegalStateException("Unroutable notification");
        db.update("UPDATE tb_notification_outbox SET published_at=? WHERE event_id=? AND published_at IS NULL",Timestamp.from(clock.instant()),id);
        metrics.counter("notification.outbox.published").increment();
        org.slf4j.LoggerFactory.getLogger(OutboxRelay.class).info("notification_published event_id={} correlation_id={}",id,envelope.path("correlationId").asText());
      }catch(Exception ignored) {
        int attempts=((Number)row.get("attempts")).intValue();
        long delay=Math.min(120,5L*(1L<<Math.min(attempts,5)))+java.util.concurrent.ThreadLocalRandom.current().nextInt(3);
        db.update("UPDATE tb_notification_outbox SET attempts=attempts+1,next_attempt_at=? WHERE event_id=? AND published_at IS NULL",Timestamp.from(clock.instant().plusSeconds(delay)),id);
        metrics.counter("notification.outbox.publish.errors").increment();
      }
    }
  }
}
