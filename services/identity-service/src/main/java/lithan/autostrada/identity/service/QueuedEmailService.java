package lithan.autostrada.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Durable command committed with reset issuance. No SMTP or broker I/O in this transaction. */
@Service
public class QueuedEmailService implements EmailService {
  private final JdbcTemplate db;private final ObjectMapper json;private final DeliveryCipher cipher;private final Clock clock;
  public QueuedEmailService(JdbcTemplate db,ObjectMapper json,DeliveryCipher cipher,Clock clock){this.db=db;this.json=json;this.cipher=cipher;this.clock=clock;}
  @Override @Transactional(propagation=Propagation.MANDATORY)
  public void sendReset(String to,String resetUrl,Instant expiresAt) {
    String id=UUID.randomUUID().toString();
    // Timestamp precision is normalized so authenticated encryption context survives DB round trips.
    Instant expires=expiresAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    try {
      String encrypted=cipher.encrypt(json.writeValueAsString(Map.of("deliveryId",id,"recipientEmail",to,"template","password-reset-v1",
          "resetUrl",resetUrl,"expiresAt",expires.toString())),id+"|"+expires);
      db.update("INSERT INTO tb_delivery_outbox(event_id,encrypted_payload,expires_at,created_at,next_attempt_at) VALUES (?,?,?,?,?)",
          id,encrypted,Timestamp.from(expires),Timestamp.from(clock.instant()),Timestamp.from(clock.instant()));
    }catch(com.fasterxml.jackson.core.JsonProcessingException ignored){throw new IllegalStateException("Reset delivery serialization failed");}
  }
}
