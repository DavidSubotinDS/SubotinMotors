package lithan.autostrada.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real RabbitMQ/MySQL probe, executed only inside the disposable Compose network. */
public final class BrokerProbe {
  static final ObjectMapper JSON=new ObjectMapper();
  public static void main(String[] args) throws Exception {
    String schema=System.getenv("E2E_NOTIFICATION_DATABASE");
    if(schema==null||!schema.matches("notification_[a-f0-9]{32}"))throw new IllegalStateException("Disposable schema required");
    try(var db=DriverManager.getConnection("jdbc:mysql://mysql:3306/"+schema+"?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC",
        System.getenv("NOTIFICATION_DB_USERNAME"),System.getenv("NOTIFICATION_DB_PASSWORD"));var operator=connect("operator");var admin=operator.createChannel()) {
      var valid=event(UUID.randomUUID().toString(),9999997);
      publish("backend","autostrada.events","marketplace.auction-ending-soon.v1",JSON.writeValueAsBytes(valid));
      await(()->count(db,9999997)==1,"real delivery");
      publish("backend","autostrada.events","marketplace.auction-ending-soon.v1",JSON.writeValueAsBytes(valid));
      var second=event(UUID.randomUUID().toString(),9999997);publish("backend","autostrada.events","marketplace.auction-ending-soon.v1",JSON.writeValueAsBytes(second));
      await(()->inbox(db,(String)second.get("eventId")),"business dedupe");
      require(count(db,9999997)==1,"Duplicate notification inserted");
      for(String user:List.of("backend","identity")) {
        boolean denied=false;
        try {publish(user,user.equals("backend")?"autostrada.commands":"autostrada.events",
            user.equals("backend")?"identity.password-reset-delivery.v1":"marketplace.auction-ending-soon.v1",JSON.writeValueAsBytes(valid));}
        catch(Exception expected){denied=true;}
        require(denied,"Cross-producer broker permission allowed");
      }
      String poisonId=UUID.randomUUID().toString();var poison=event(poisonId,9999996);poison.put("schemaVersion",2);
      publish("backend","autostrada.events","marketplace.auction-ending-soon.v1",JSON.writeValueAsBytes(poison));
      await(()->admin.queueDeclarePassive("notification.business.v1.dlq").getMessageCount()>0,"poison quarantine");
      var dead=admin.basicGet("notification.business.v1.dlq",false);require(dead!=null,"Missing quarantine message");
      require(JSON.readTree(dead.getBody()).path("eventId").asText().equals(poisonId),"Poison identity changed");
      // Explicit test repair of malformed producer schema, retaining the business/event identity.
      poison.put("schemaVersion",1);publish("operator","autostrada.notification-routing","notification.business.v1",JSON.writeValueAsBytes(poison));
      admin.basicAck(dead.getEnvelope().getDeliveryTag(),false);
      await(()->count(db,9999996)==1,"quarantine repair and redrive");
      try(var s=db.createStatement()){s.execute("ALTER TABLE tb_notification ADD CONSTRAINT s6_probe_transient CHECK (id_user<>9999995)");}
      var retry=event(UUID.randomUUID().toString(),9999995);
      publish("backend","autostrada.events","marketplace.auction-ending-soon.v1",JSON.writeValueAsBytes(retry));
      await(()->admin.queueDeclarePassive("notification.business.v1.retry.1").getMessageCount()>0,"durable delayed retry");
      try(var s=db.createStatement()){s.execute("ALTER TABLE tb_notification DROP CHECK s6_probe_transient");}
      await(()->count(db,9999995)==1,"retry recovery");
      System.out.println("Real RabbitMQ/MySQL probe passed: delivery, event/business dedupe, producer isolation, poison quarantine, repaired redrive and delayed retry.");
    }catch(Exception ignored){System.err.println("S6 broker probe failed; inspect retained private logs. No payload is printed.");System.exit(1);}
  }
  static com.rabbitmq.client.Connection connect(String user) throws Exception {
    var f=new ConnectionFactory();f.setHost("rabbitmq");f.setVirtualHost("autostrada");f.setUsername(user);
    f.setPassword(System.getenv("RABBITMQ_"+user.toUpperCase()+"_PASSWORD"));f.setConnectionTimeout(2000);f.setHandshakeTimeout(2000);f.setChannelRpcTimeout(2000);f.setAutomaticRecoveryEnabled(false);
    return f.newConnection("s6-probe-"+user);
  }
  static void publish(String user,String exchange,String key,byte[] body) throws Exception {
    try(var c=connect(user);var channel=c.createChannel()) {
      AtomicBoolean returned=new AtomicBoolean();channel.addReturnListener((code,text,e,k,p,b)->returned.set(true));channel.confirmSelect();
      channel.basicPublish(exchange,key,true,new AMQP.BasicProperties.Builder().deliveryMode(2).contentType("application/json").build(),body);
      channel.waitForConfirmsOrDie(2000);require(!returned.get(),"Unroutable probe");
    }
  }
  static Map<String,Object> event(String id,int user) {
    var e=InboxTests.event(id);
    var p=new LinkedHashMap<String,Object>((Map<String,Object>)e.get("payload"));p.put("recipientId",Integer.toString(user));p.put("dedupeKey",user+":10:ENDING_SOON");
    e.put("payload",p);e.put("aggregateId",p.get("dedupeKey"));e.put("occurredAt",java.time.Instant.now().toString());return e;
  }
  static long count(java.sql.Connection db,int user) throws Exception {
    try(var s=db.prepareStatement("SELECT COUNT(*) FROM tb_notification WHERE id_user=?")){s.setInt(1,user);try(var r=s.executeQuery()){r.next();return r.getLong(1);}}
  }
  static boolean inbox(java.sql.Connection db,String id) throws Exception {
    try(var s=db.prepareStatement("SELECT COUNT(*) FROM tb_message_inbox WHERE event_id=?")){s.setString(1,id);try(var r=s.executeQuery()){r.next();return r.getLong(1)>0;}}
  }
  interface Check {boolean get()throws Exception;}
  static void await(Check check,String description)throws Exception {
    long end=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(25);
    while(System.nanoTime()<end){if(check.get())return;Thread.sleep(100);}throw new IllegalStateException("Timed out: "+description);
  }
  static void require(boolean condition,String description){if(!condition)throw new IllegalStateException(description);}
}
