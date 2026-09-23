package e2e;

import java.time.*;
import java.util.*;
import lithan.autostrada.notification.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.*;

/** Guarded test classpath only. Production artifacts never contain this launcher or mailbox. */
public class E2eApplication {
  public static void main(String[] ignored) {
    String token=System.getenv("E2E_CONTROL_TOKEN");if(token==null||!token.matches("[a-f0-9]{64}"))throw new IllegalStateException("Per-run control token required");
    String schema=System.getenv("E2E_NOTIFICATION_DATABASE");
    boolean mysql=schema!=null;
    if(mysql&&!schema.matches("notification_[a-f0-9]{32}"))throw new IllegalStateException("Disposable schema required");
    String url=mysql ? "jdbc:mysql://mysql:3306/"+schema+"?serverTimezone=UTC&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&allowPublicKeyRetrieval=true&useSSL=false"
        : "jdbc:h2:mem:e2e_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    new SpringApplication(NotificationApplication.class,TestConfig.class).run("--spring.config.location=classpath:/application.properties",
        "--spring.datasource.url="+url,"--spring.datasource.username="+(mysql?System.getenv("NOTIFICATION_DB_USERNAME"):"sa"),
        "--spring.datasource.password="+(mysql?System.getenv("NOTIFICATION_DB_PASSWORD"):""),
        "--server.address="+(mysql?"0.0.0.0":"127.0.0.1"),"--server.port="+(mysql?"8080":"18083"),
        "--notification.delivery.enabled=false","--notification.broker.enabled="+mysql);
  }
  @TestConfiguration(proxyBeanMethods=false)
  static class TestConfig {
    @Bean @Primary TestClock testClock(){return new TestClock();}
    @Bean Mailbox mailbox(){return new Mailbox();}
    @Bean DeliveryWorker testDelivery(JdbcTemplate db,DeliveryCipher cipher,com.fasterxml.jackson.databind.ObjectMapper json,TestClock clock,
        org.springframework.beans.factory.ObjectProvider<org.springframework.mail.javamail.JavaMailSender> smtp,io.micrometer.core.instrument.MeterRegistry metrics,Mailbox mailbox){
      return new DeliveryWorker(db,cipher,json,clock,smtp,"smtp","test@e2e.invalid",metrics){
        @Override protected void deliverMessage(String to,String subject,String body){mailbox.messages.put(to,body);}
      };
    }
    @Bean Controls controls(JdbcTemplate db,Inbox inbox,TestClock clock,Mailbox mailbox){return new Controls(db,inbox,clock,mailbox);}
    @Bean @org.springframework.core.annotation.Order(0) SecurityFilterChain controlsSecurity(HttpSecurity http)throws Exception {
      return http.securityMatcher("/__e2e/**").csrf(c->c.disable()).authorizeHttpRequests(a->a.anyRequest().access((auth,context)->
          new AuthorizationDecision(System.getenv("E2E_CONTROL_TOKEN").equals(context.getRequest().getHeader("X-E2E-Control"))))).build();
    }
  }
  static class TestClock extends Clock {
    volatile Instant now=Instant.parse("2030-06-15T10:00:00Z");
    public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return Clock.fixed(now,zone);}public Instant instant(){return now;}
  }
  static class Mailbox {final Map<String,String> messages=new java.util.concurrent.ConcurrentHashMap<>();}
  @RestController static class Controls {
    final JdbcTemplate db;final Inbox inbox;final TestClock clock;final Mailbox mailbox;
    Controls(JdbcTemplate db,Inbox inbox,TestClock clock,Mailbox mailbox){this.db=db;this.inbox=inbox;this.clock=clock;this.mailbox=mailbox;}
    @GetMapping("/__e2e/ready") Map<String,String> ready(){return Map.of("mode","isolated-e2e");}
    @GetMapping("/__e2e/mail") Map<String,String> mail(@RequestParam String recipient){return Map.of("body",mailbox.messages.getOrDefault(recipient,""));}
    @PostMapping("/__e2e/clock") Map<String,String> clock(@RequestBody Map<String,String> body){clock.now=Instant.parse(body.get("instant"));return Map.of("instant",clock.now.toString());}
    @PostMapping("/__e2e/events") void accept(@RequestBody byte[] body,@RequestParam(defaultValue="false") boolean sensitive){inbox.accept(body,sensitive);}
    @PostMapping("/__e2e/reset") Map<String,String> reset() throws Exception {
      try(var connection=db.getDataSource().getConnection()) {
        String url=connection.getMetaData().getURL();
        if(!url.startsWith("jdbc:h2:mem:e2e_") && !(url.startsWith("jdbc:mysql://mysql:3306/notification_")
            && connection.getCatalog().equals(System.getenv("E2E_NOTIFICATION_DATABASE"))
            && System.getenv("E2E_CONTROL_TOKEN").equals(db.queryForObject("SELECT token FROM e2e_guard",String.class))))
          throw new IllegalStateException("Not an owned test database");
      }
      for(String table:List.of("tb_notification","tb_message_inbox","tb_delivery"))db.update("DELETE FROM "+table);
      mailbox.messages.clear();clock.now=Instant.parse("2030-06-15T10:00:00Z");return ready();
    }
  }
}
