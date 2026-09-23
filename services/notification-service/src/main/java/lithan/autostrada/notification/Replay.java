package lithan.autostrada.notification;

import com.rabbitmq.client.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Operator-only bounded redrive. Preserves event IDs and never clears destination dedupe. */
public final class Replay {
  public static void main(String[] args) throws Exception {
    if(args.length!=2 || !Set.of("notification.business.v1","notification.delivery.v1").contains(args[0]) || args[1].isBlank())
      throw new IllegalArgumentException("Supply queue and an audit reason; private operator credentials are required");
    var factory=new ConnectionFactory();factory.setHost(System.getenv("RABBITMQ_HOST"));factory.setUsername("operator");
    factory.setPassword(System.getenv("RABBITMQ_OPERATOR_PASSWORD"));factory.setVirtualHost("autostrada");
    factory.setConnectionTimeout(2000);factory.setHandshakeTimeout(2000);factory.setChannelRpcTimeout(2000);factory.setAutomaticRecoveryEnabled(false);
    int replayed=0,expired=0;
    try(var connection=factory.newConnection("operator-redrive");var channel=connection.createChannel()) {
      channel.confirmSelect();
      for(int i=0;i<100;i++) {
        var message=channel.basicGet(args[0]+".dlq",false);if(message==null)break;
        if(args[0].contains("delivery")) {
          try {
            String end=new ObjectMapper().readTree(message.getBody()).path("payload").path("expiresAt").asText();
            if(!Instant.parse(end).isAfter(Instant.now())){channel.basicAck(message.getEnvelope().getDeliveryTag(),false);expired++;continue;}
          }catch(RuntimeException ignored){throw new IllegalStateException("Cannot safely replay malformed delivery expiry");}
        }
        AtomicBoolean returned=new AtomicBoolean();ReturnListener listener=(code,text,exchange,routing,props,body)->returned.set(true);
        channel.addReturnListener(listener);
        Map<String,Object> headers=new HashMap<>();if(message.getProps().getHeaders()!=null)headers.putAll(message.getProps().getHeaders());headers.put("attempt",0);
        channel.basicPublish("autostrada.notification-routing",args[0],true,message.getProps().builder().headers(headers).deliveryMode(2).build(),message.getBody());
        channel.waitForConfirmsOrDie(2000);channel.removeReturnListener(listener);if(returned.get())throw new IllegalStateException("Unroutable redrive");
        channel.basicAck(message.getEnvelope().getDeliveryTag(),false);replayed++;
      }
    }catch(Exception ignored){System.err.println("Redrive failed; unacknowledged messages remain queued. Payload omitted.");System.exit(1);}
    // The reason is deliberately not echoed: operators can accidentally paste sensitive data.
    System.out.println("Operator redrive completed; record the supplied reason in the private operations log. replayed="+replayed+" expired="+expired);
  }
}
