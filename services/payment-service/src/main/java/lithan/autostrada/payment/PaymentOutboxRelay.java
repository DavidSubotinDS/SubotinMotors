package lithan.autostrada.payment;

import com.rabbitmq.client.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="payment.broker.enabled",havingValue="true",matchIfMissing=true)
class PaymentOutboxRelay {
  private final JdbcTemplate db;private final Clock clock;private final ConnectionFactory factory=new ConnectionFactory();
  PaymentOutboxRelay(JdbcTemplate db,Clock clock,@Value("${payment.broker.host}")String host,@Value("${payment.broker.port}")int port,
      @Value("${payment.broker.username}")String user,@Value("${payment.broker.password}")String password,@Value("${payment.broker.vhost}")String vhost){
    this.db=db;this.clock=clock;factory.setHost(host);factory.setPort(port);factory.setUsername(user);factory.setPassword(password);factory.setVirtualHost(vhost);
    factory.setAutomaticRecoveryEnabled(false);factory.setConnectionTimeout(1000);factory.setHandshakeTimeout(2000);factory.setChannelRpcTimeout(2000);
  }
  @Scheduled(fixedDelayString="${payment.outbox.poll-ms:1000}") void relay(){
    List<Map<String,Object>> rows;try{rows=db.queryForList("SELECT event_id,routing_key,payload_json,correlation_id,attempt_count FROM payment_outbox WHERE status='PENDING' ORDER BY created_at LIMIT 20");}catch(RuntimeException unavailable){return;}
    for(var row:rows){String id=(String)row.get("event_id");try(var connection=factory.newConnection("payment-outbox");var channel=connection.createChannel()){
      AtomicBoolean returned=new AtomicBoolean();channel.addReturnListener((code,text,exchange,routing,props,body)->returned.set(true));channel.confirmSelect();
      channel.basicPublish("autostrada.events",(String)row.get("routing_key"),true,new AMQP.BasicProperties.Builder().contentType("application/json").deliveryMode(2).messageId(id).correlationId((String)row.get("correlation_id")).build(),((String)row.get("payload_json")).getBytes(StandardCharsets.UTF_8));
      channel.waitForConfirmsOrDie(2000);if(returned.get())throw new IllegalStateException("Unroutable payment result");
      db.update("UPDATE payment_outbox SET status='PUBLISHED',published_at=? WHERE event_id=? AND status='PENDING'",Timestamp.from(clock.instant()),id);
    }catch(Exception failure){db.update("UPDATE payment_outbox SET attempt_count=attempt_count+1 WHERE event_id=? AND status='PENDING'",id);}}
  }
}
