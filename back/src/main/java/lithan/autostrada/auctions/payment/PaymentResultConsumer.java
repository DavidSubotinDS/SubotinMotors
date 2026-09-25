package lithan.autostrada.auctions.payment;

import com.rabbitmq.client.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="payment.results.enabled",havingValue="true",matchIfMissing=true)
class PaymentResultConsumer {
  private final ConnectionFactory factory=new ConnectionFactory();private final PaymentResultInbox inbox;
  PaymentResultConsumer(PaymentResultInbox inbox,@Value("${notification.broker.host:localhost}")String host,@Value("${notification.broker.port:5672}")int port,@Value("${notification.broker.username:backend}")String user,@Value("${notification.broker.password:}")String password,@Value("${notification.broker.vhost:autostrada}")String vhost){
    this.inbox=inbox;factory.setHost(host);factory.setPort(port);factory.setUsername(user);factory.setPassword(password);factory.setVirtualHost(vhost);factory.setAutomaticRecoveryEnabled(false);factory.setConnectionTimeout(1000);factory.setHandshakeTimeout(2000);factory.setChannelRpcTimeout(2000);
  }
  @Scheduled(fixedDelayString="${payment.results.poll-ms:1000}")void poll(){
    try(var connection=factory.newConnection("backend-payment-results");var channel=connection.createChannel()){
      channel.confirmSelect();
      for(String queue:List.of("commerce.payment-results.v1","marketplace.payment-results.v1")) {
        // The counter bounds broker work per queue and poll.
        for(int i=0;i<20;i++){
        var message=channel.basicGet(queue,false);if(message==null)break;
        try{inbox.accept(message.getBody());channel.basicAck(message.getEnvelope().getDeliveryTag(),false);}
        catch(RuntimeException failure){retry(channel,queue,message);}
        }
      }
    }catch(Exception ignored){ }
  }
  private void retry(Channel channel,String queue,GetResponse message)throws Exception{
    Map<String,Object> headers=new HashMap<>();if(message.getProps().getHeaders()!=null)headers.putAll(message.getProps().getHeaders());
    int attempt=headers.get("attempt")instanceof Number n?Math.max(0,n.intValue()):0;String destination=attempt>=3?queue+".dlq":queue+".retry."+(attempt+1);headers.put("attempt",attempt+1);
    AtomicBoolean returned=new AtomicBoolean();ReturnListener listener=(code,text,exchange,routing,props,body)->returned.set(true);channel.addReturnListener(listener);
    channel.basicPublish("autostrada.payment-routing",destination,true,message.getProps().builder().deliveryMode(2).headers(headers).build(),message.getBody());channel.waitForConfirmsOrDie(2000);channel.removeReturnListener(listener);
    if(returned.get())throw new IllegalStateException("Unroutable payment retry");channel.basicAck(message.getEnvelope().getDeliveryTag(),false);
  }
}
