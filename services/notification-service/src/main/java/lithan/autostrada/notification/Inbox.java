package lithan.autostrada.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Local inbox and business uniqueness commit together before a broker acknowledgement. */
@Service
public class Inbox {
  public static final String ENDING = "marketplace.auction-ending-soon.v1";
  public static final String RESET = "identity.password-reset-delivery.v1";
  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final TransactionTemplate tx;
  private final Clock clock;
  public Inbox(JdbcTemplate db, ObjectMapper json, TransactionTemplate tx, Clock clock) {
    this.db=db; this.json=json; this.tx=tx; this.clock=clock;
  }
  public void accept(byte[] bytes, boolean sensitive) {
    JsonNode event;
    try {
      if (bytes.length>65536) throw new IllegalArgumentException();
      event=json.readTree(bytes);
      UUID.fromString(required(event,"eventId",36));
      if (event.path("schemaVersion").asInt()!=1 || event.path("aggregateVersion").asInt()!=1) throw new IllegalArgumentException();
      Instant.parse(required(event,"occurredAt",40));
      UUID.fromString(required(event,"correlationId",36));
      required(event,"aggregateId",160);
      if (!event.has("causationId")) throw new IllegalArgumentException();
      if (!(sensitive ? RESET : ENDING).equals(required(event,"eventType",80))) throw new IllegalArgumentException();
      if (!(sensitive ? "identity-service" : "legacy-backend").equals(required(event,"producer",40))) throw new IllegalArgumentException();
      if (!(sensitive ? "PasswordResetDelivery" : "AuctionNotification").equals(required(event,"aggregateType",40))) throw new IllegalArgumentException();
      validatePayload(event.path("payload"),sensitive);
      if(sensitive && (!event.path("eventId").asText().equals(event.path("payload").path("deliveryId").asText())
          || !event.path("eventId").asText().equals(event.path("aggregateId").asText())))throw new InvalidMessage();
    } catch (Exception ignored) { throw new InvalidMessage(); }
    String id=event.path("eventId").asText();
    String consumer=sensitive ? "notification.delivery.v1" : "notification.business.v1";
    JsonNode payload=event.path("payload");
    // One consumer per queue initially. A duplicate race must roll back and redeliver,
    // never acknowledge a transaction whose business write did not commit.
    tx.executeWithoutResult(status -> {
      if (db.queryForObject("SELECT COUNT(*) FROM tb_message_inbox WHERE consumer_name=? AND event_id=?",Long.class,consumer,id)>0) return;
      if (sensitive) {
        Instant expires=Instant.parse(payload.path("expiresAt").asText());
        if (db.queryForObject("SELECT COUNT(*) FROM tb_delivery WHERE delivery_id=?",Long.class,id)==0)
          db.update("INSERT INTO tb_delivery(delivery_id,encrypted_payload,expires_at,state,next_attempt_at,created_at) VALUES (?,?,?,?,?,?)",
              id,expires.isAfter(clock.instant()) ? payload.path("encryptedPayload").asText() : null,
              Timestamp.from(expires),expires.isAfter(clock.instant()) ? "PENDING" : "EXPIRED",
              Timestamp.from(clock.instant()),Timestamp.from(clock.instant()));
      } else if (db.queryForObject("SELECT COUNT(*) FROM tb_notification WHERE dedupe_key=?",Long.class,payload.path("dedupeKey").asText())==0) {
        db.update("INSERT INTO tb_notification(id_user,id_car,notification_type,message,created_at,auction_snapshot,dedupe_key) VALUES (?,?,?,?,?,?,?)",
            Integer.parseInt(payload.path("recipientId").asText()),Integer.parseInt(payload.path("auctionId").asText()),
            "AUCTION_ENDING_SOON",payload.path("message").asText(),Timestamp.from(Instant.parse(event.path("occurredAt").asText())),
            payload.path("auctionSnapshot").toString(),payload.path("dedupeKey").asText());
      }
      db.update("INSERT INTO tb_message_inbox(consumer_name,event_id,received_at) VALUES (?,?,?)",consumer,id,Timestamp.from(clock.instant()));
    });
    org.slf4j.LoggerFactory.getLogger(Inbox.class).info("notification_consumed event_id={} correlation_id={} kind={}",
        id,event.path("correlationId").asText(),sensitive?"delivery":"business");
  }
  private void validatePayload(JsonNode p,boolean sensitive) {
    if (sensitive) {
      UUID.fromString(required(p,"deliveryId",36));
      Instant.parse(required(p,"expiresAt",40));
      required(p,"encryptedPayload",16000);
      return;
    }
    int recipient=Integer.parseInt(required(p,"recipientId",10));
    int car=Integer.parseInt(required(p,"auctionId",10));
    if (recipient<=0 || car<=0 || !"AUCTION_ENDING_SOON".equals(required(p,"notificationType",50))) throw new InvalidMessage();
    if (! (recipient+":"+car+":ENDING_SOON").equals(required(p,"dedupeKey",160))) throw new InvalidMessage();
    required(p,"message",500);
    JsonNode a=p.path("auctionSnapshot");
    if (a.path("id").asInt()!=car) throw new InvalidMessage();
    Set<String> fields=Set.of("id","make","model","year","price","status","statusLabel","auctionEndTime",
        "auctionEndTimeEpochMillis","imageUrl","imageUrls","sellerDisplayName");
    a.fieldNames().forEachRemaining(f->{if(!fields.contains(f))throw new InvalidMessage();});
    for(String field:fields) if(!a.has(field))throw new InvalidMessage();
    if(a.toString().length()>12000)throw new InvalidMessage();
  }
  private static String required(JsonNode node,String key,int max) {
    JsonNode value=node.path(key);
    if(!value.isTextual() || value.asText().isBlank() || value.asText().length()>max)throw new InvalidMessage();
    return value.asText();
  }
  public List<Map<String,Object>> list(int user) {
    return db.query("SELECT * FROM tb_notification WHERE id_user=? ORDER BY created_at DESC,id_notification DESC",(rs,n)->{
      Map<String,Object> row=new LinkedHashMap<>();
      row.put("idNotification",rs.getInt("id_notification"));row.put("notificationType",rs.getString("notification_type"));
      row.put("message",rs.getString("message"));row.put("createdAt",rs.getTimestamp("created_at").toLocalDateTime());
      var read=rs.getTimestamp("read_at");row.put("readAt",read==null ? null : read.toLocalDateTime());row.put("read",read!=null);
      try { row.put("auction",json.readTree(rs.getString("auction_snapshot"))); }
      catch(Exception ignored) { throw new IllegalStateException("Invalid stored notification snapshot"); }
      return row;
    },user);
  }
  public long unread(int user) { return db.queryForObject("SELECT COUNT(*) FROM tb_notification WHERE id_user=? AND read_at IS NULL",Long.class,user); }
  public void read(int user,int id) {
    var owners=db.queryForList("SELECT id_user FROM tb_notification WHERE id_notification=?",Integer.class,id);
    if(owners.isEmpty())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
    if(owners.get(0)!=user)throw new org.springframework.security.access.AccessDeniedException("Notification belongs to another user");
    db.update("UPDATE tb_notification SET read_at=? WHERE id_notification=? AND id_user=? AND read_at IS NULL",Timestamp.from(clock.instant()),id,user);
  }
  public void readAll(int user) { db.update("UPDATE tb_notification SET read_at=? WHERE id_user=? AND read_at IS NULL",Timestamp.from(clock.instant()),user); }
  public static final class InvalidMessage extends RuntimeException { public InvalidMessage(){super("Invalid message contract");} }
}
