package lithan.autostrada.auctions.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import lithan.autostrada.auctions.service.*;

@Component
public class PaymentResultInbox {
  private final JdbcTemplate db;private final ObjectMapper json;private final Clock clock;
  private final StoreOrderServiceImpl store;private final ListingDepositServiceImpl deposits;
  PaymentResultInbox(JdbcTemplate db,ObjectMapper json,Clock clock,StoreOrderServiceImpl store,ListingDepositServiceImpl deposits){this.db=db;this.json=json;this.clock=clock;this.store=store;this.deposits=deposits;}
  @Transactional public void accept(byte[] bytes){
    try{
      var root=json.readTree(bytes);String eventId=text(root,"eventId"),type=text(root,"eventType");var p=root.path("payload");
      String attempt=text(p,"attemptId"),business=text(p,"businessType"),status=text(p,"status"),intent=p.path("providerPaymentIntentId").asText(null);
      if(!type.matches("payment\\.(succeeded|failed|expired)\\.v1"))throw new Invalid("Unsupported event type");
      if(!status.matches("SUCCEEDED|FAILED|EXPIRED"))throw new Invalid("Unsupported payment status");
      if(attempt.length()!=36)throw new Invalid("Invalid attempt identifier length");
      try{db.update("INSERT INTO tb_payment_result_inbox(event_id,event_type,attempt_id,status,received_at) VALUES (?,?,?,'RECEIVED',?)",eventId,type,attempt,Timestamp.from(clock.instant()));}
      catch(DuplicateKeyException duplicate){return;}
      if("STORE_ORDER".equals(business))store.processPaymentResult(attempt,status,intent);
      else if("LISTING_DEPOSIT".equals(business))deposits.processPaymentResult(attempt,status,intent);
      else throw new Invalid();
      db.update("UPDATE tb_payment_result_inbox SET status='PROCESSED',processed_at=? WHERE event_id=?",Timestamp.from(clock.instant()),eventId);
    }catch(Invalid invalid){throw invalid;}catch(Exception malformed){throw new Invalid(malformed);}
  }
  private static String text(com.fasterxml.jackson.databind.JsonNode node,String field){String v=node.path(field).asText("");if(v.isBlank()||v.length()>100)throw new Invalid();return v;}
  static final class Invalid extends RuntimeException{
    Invalid() { }
    Invalid(String message) { super(message); }
    Invalid(Throwable cause) { super(cause); }
  }
}
