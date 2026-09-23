package lithan.autostrada.auctions.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import lithan.autostrada.auctions.entity.Car;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Called in the eligibility transaction; never performs network I/O. */
@Service
public class EndingSoonOutbox {
  private final JdbcTemplate db;
  private final ObjectMapper json;
  public EndingSoonOutbox(JdbcTemplate db,ObjectMapper json) {this.db=db;this.json=json;}
  @Transactional(propagation=Propagation.MANDATORY)
  public boolean append(int recipient,Car car,LocalDateTime now,Duration window) {
    // Serialize eligibility for this auction, including concurrent follow/scanner requests.
    var current=db.queryForMap("SELECT status,auction_end_time FROM tb_car WHERE id_car=? FOR UPDATE",car.getIdCar());
    var deadline=((Timestamp)current.get("auction_end_time")).toLocalDateTime();
    if(!"ACTIVE".equals(current.get("status")) || !deadline.isAfter(now) || deadline.isAfter(now.plus(window)))return false;
    car.setStatus((String)current.get("status"));car.setAuctionEndTime(deadline);
    String key=recipient+":"+car.getIdCar()+":ENDING_SOON";
    if(db.queryForObject("SELECT COUNT(*) FROM tb_notification_outbox WHERE dedupe_key=?",Long.class,key)>0)return false;
    String eventId=UUID.randomUUID().toString();
    Map<String,Object> snapshot=new LinkedHashMap<>();
    snapshot.put("id",car.getIdCar());snapshot.put("make",car.getMake());snapshot.put("model",car.getModel());
    snapshot.put("year",car.getYear());snapshot.put("price",car.getPrice());snapshot.put("status",car.auctionStatusAt(now));
    snapshot.put("statusLabel",car.auctionStatusLabelAt(now));snapshot.put("auctionEndTime",car.getAuctionEndTimeDisplay());
    snapshot.put("auctionEndTimeEpochMillis",car.getAuctionEndTimeEpochMillis());
    // Public image URLs keep broker messages bounded; no embedded image bytes or private profile fields.
    snapshot.put("imageUrl","/api/public/auctions/"+car.getIdCar()+"/notification-image");
    snapshot.put("imageUrls",List.of("/api/public/auctions/"+car.getIdCar()+"/notification-image"));
    snapshot.put("sellerDisplayName",null);
    Map<String,Object> payload=new LinkedHashMap<>();payload.put("auctionId",Integer.toString(car.getIdCar()));
    payload.put("recipientId",Integer.toString(recipient));payload.put("notificationType","AUCTION_ENDING_SOON");
    payload.put("message",car.getMake()+" "+car.getModel()+" is ending soon.");payload.put("dedupeKey",key);
    payload.put("auctionSnapshot",snapshot);
    Map<String,Object> event=new LinkedHashMap<>();event.put("eventId",eventId);event.put("eventType","marketplace.auction-ending-soon.v1");
    event.put("schemaVersion",1);event.put("occurredAt",now.toInstant(ZoneOffset.UTC).toString());event.put("producer","legacy-backend");
    event.put("aggregateType","AuctionNotification");event.put("aggregateId",key);event.put("aggregateVersion",1);
    String correlation=UUID.randomUUID().toString();
    var attributes=org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
    if(attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
      String requestId=servlet.getRequest().getHeader("X-Request-ID");
      if(requestId!=null && requestId.matches("[a-f0-9-]{36}"))correlation=requestId;
      String trace=servlet.getRequest().getHeader("traceparent");
      if(trace!=null && trace.matches("00-[a-f0-9]{32}-[a-f0-9]{16}-[a-f0-9]{2}"))event.put("traceparent",trace);
    }
    event.put("correlationId",correlation);event.put("causationId",null);event.put("payload",payload);
    try {
      db.update("INSERT INTO tb_notification_outbox(event_id,dedupe_key,payload,created_at,next_attempt_at) VALUES (?,?,?,?,?)",
          eventId,key,json.writeValueAsString(event),Timestamp.valueOf(now),Timestamp.valueOf(now));
    }catch(com.fasterxml.jackson.core.JsonProcessingException ignored){throw new IllegalStateException("Notification serialization failed");}
    return true;
  }
}
