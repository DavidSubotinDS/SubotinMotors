package lithan.autostrada.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="notification.delivery.enabled",havingValue="true",matchIfMissing=true)
public class DeliveryWorker {
  private final JdbcTemplate db;private final DeliveryCipher cipher;private final ObjectMapper json;private final Clock clock;
  private final ObjectProvider<JavaMailSender> smtp;private final String mode,from;private final MeterRegistry metrics;
  public DeliveryWorker(JdbcTemplate db,DeliveryCipher cipher,ObjectMapper json,Clock clock,ObjectProvider<JavaMailSender> smtp,
      @Value("${app.mail.mode:log}") String mode,@Value("${app.mail.from}") String from,MeterRegistry metrics) {
    this.db=db;this.cipher=cipher;this.json=json;this.clock=clock;this.smtp=smtp;this.mode=mode;this.from=from;this.metrics=metrics;
    if(!java.util.Set.of("log","smtp").contains(mode))throw new IllegalArgumentException("Unsupported mail mode");
  }
  @Scheduled(fixedDelayString="${notification.delivery.poll-ms:1000}")
  public void deliver() {
    Instant now=clock.instant();
    db.update("UPDATE tb_delivery SET encrypted_payload=NULL,state='EXPIRED' WHERE expires_at<=? AND encrypted_payload IS NOT NULL",Timestamp.from(now));
    var rows=db.queryForList("SELECT delivery_id,encrypted_payload,expires_at,attempts FROM tb_delivery WHERE state='PENDING' AND next_attempt_at<=? ORDER BY created_at LIMIT 20",Timestamp.from(now));
    for(var row:rows) {
      String id=(String)row.get("delivery_id");Instant expires=((Timestamp)row.get("expires_at")).toInstant();
      if(!expires.isAfter(clock.instant()))continue;
      try {
        var payload=json.readTree(cipher.decrypt((String)row.get("encrypted_payload"),id+"|"+expires));
        if(!"password-reset-v1".equals(payload.path("template").asText()) || !id.equals(payload.path("deliveryId").asText())
            || !expires.equals(Instant.parse(payload.path("expiresAt").asText())))throw new Inbox.InvalidMessage();
        String link=payload.path("resetUrl").asText();
        var uri=java.net.URI.create(link);
        if(!java.util.Set.of("http","https").contains(uri.getScheme()) || uri.getHost()==null || !"/reset-password".equals(uri.getPath()))throw new Inbox.InvalidMessage();
        if(!expires.isAfter(clock.instant()))continue;
        deliverMessage(payload.path("recipientEmail").asText(),"Reset your Autostrada Auctions password",
            "Use this link to reset your password. It expires at "+expires+":\n"+link);
        db.update("UPDATE tb_delivery SET state=?,encrypted_payload=NULL,attempts=attempts+1 WHERE delivery_id=?",mode.equals("log")?"SUPPRESSED":"SENT",id);
        metrics.counter("notification.delivery.completed","mode",mode).increment();
      }catch(Exception failure) {
        int attempts=((Number)row.get("attempts")).intValue()+1;
        boolean terminal=failure instanceof Inbox.InvalidMessage || attempts>=4;
        long delay=attempts==1?5:attempts==2?30:120;
        db.update("UPDATE tb_delivery SET state=?,attempts=?,next_attempt_at=? WHERE delivery_id=?",terminal?"FAILED":"PENDING",attempts,Timestamp.from(clock.instant().plusSeconds(delay)),id);
        metrics.counter("notification.delivery.errors").increment();
      }
    }
  }
  protected void deliverMessage(String to,String subject,String body) {
    if(mode.equals("log")) {
      org.slf4j.LoggerFactory.getLogger(DeliveryWorker.class).info("Reset delivery suppressed; configure SMTP or an isolated test sink. Payload omitted.");return;
    }
    var message=new SimpleMailMessage();message.setFrom(from);message.setTo(to);message.setSubject(subject);message.setText(body);
    smtp.getObject().send(message);
  }
}
