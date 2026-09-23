package lithan.autostrada.notification;

import com.rabbitmq.client.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;

/** Bounded polling permits independent HTTP startup and inbox reads during broker outages. */
@Component
@ConditionalOnProperty(name="notification.broker.enabled",havingValue="true",matchIfMissing=true)
class BrokerConsumer {
  private final ConnectionFactory factory=new ConnectionFactory();
  private final Inbox inbox;
  private final MeterRegistry metrics;
  private final java.time.Clock clock;
  private final Map<String,java.util.concurrent.atomic.AtomicInteger> depths=new HashMap<>();
  BrokerConsumer(Inbox inbox,MeterRegistry metrics,java.time.Clock clock,@Value("${notification.broker.host}") String host,
      @Value("${notification.broker.port}") int port,@Value("${notification.broker.username}") String user,
      @Value("${notification.broker.password}") String password,@Value("${notification.broker.vhost}") String vhost) {
    this.inbox=inbox;this.metrics=metrics;this.clock=clock;
    factory.setHost(host);factory.setPort(port);factory.setUsername(user);factory.setPassword(password);factory.setVirtualHost(vhost);
    factory.setConnectionTimeout(1000);factory.setHandshakeTimeout(2000);factory.setChannelRpcTimeout(2000);
    factory.setAutomaticRecoveryEnabled(false);
  }
  @Scheduled(fixedDelayString="${notification.poll-ms:1000}")
  public void poll() {
    try(var connection=factory.newConnection("notification-consumer");var channel=connection.createChannel()) {
      channel.confirmSelect();
      for(boolean sensitive:new boolean[]{false,true}) {
        String queue=sensitive ? "notification.delivery.v1" : "notification.business.v1";
        for(String suffix:List.of("", ".retry.1", ".retry.2", ".retry.3", ".dlq")) {
          String monitored=queue+suffix;
          var depth=channel.queueDeclarePassive(monitored);
          depths.computeIfAbsent(monitored,q->metrics.gauge("notification.queue.depth",List.of(io.micrometer.core.instrument.Tag.of("queue",q)),new java.util.concurrent.atomic.AtomicInteger())).set(depth.getMessageCount());
        }
        for(int i=0;i<20;i++) {
          var message=channel.basicGet(queue,false);if(message==null)break;
          try {
            inbox.accept(message.getBody(),sensitive);
            var event=new com.fasterxml.jackson.databind.ObjectMapper().readTree(message.getBody());
            long lag=Math.max(0,java.time.Duration.between(java.time.Instant.parse(event.path("occurredAt").asText()),clock.instant()).toMillis());
            metrics.timer("notification.delivery.lag","kind",sensitive?"delivery":"business").record(lag,java.util.concurrent.TimeUnit.MILLISECONDS);
            channel.basicAck(message.getEnvelope().getDeliveryTag(),false);
            metrics.counter("notification.consumed","kind",sensitive?"delivery":"business").increment();
          } catch(RuntimeException failure) {
            metrics.counter("notification.consumer.errors").increment();
            Map<String,Object> headers=new HashMap<>();
            if(message.getProps().getHeaders()!=null)headers.putAll(message.getProps().getHeaders());
            int attempt=headers.get("attempt") instanceof Number n ? Math.max(0,n.intValue()) : 0;
            String destination=failure instanceof Inbox.InvalidMessage || attempt>=3 ? queue+".dlq" : queue+".retry."+(attempt+1);
            headers.put("attempt",attempt+1);
            AtomicBoolean returned=new AtomicBoolean();
            ReturnListener listener=(code,text,exchange,routing,props,body)->returned.set(true);
            channel.addReturnListener(listener);
            var props=message.getProps().builder().deliveryMode(2).headers(headers).build();
            channel.basicPublish("autostrada.notification-routing",destination,true,props,message.getBody());
            channel.waitForConfirmsOrDie(2000);
            channel.removeReturnListener(listener);
            if(returned.get())throw new IllegalStateException("Unroutable retry");
            channel.basicAck(message.getEnvelope().getDeliveryTag(),false);
          }
        }
      }
    }catch(Exception ignored) {metrics.counter("notification.broker.failures").increment();}
  }
}
