package lithan.autostrada.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class PaymentStore {
  private final JdbcTemplate db; private final ObjectMapper json; private final Clock clock;
  PaymentStore(JdbcTemplate db,ObjectMapper json,Clock clock){this.db=db;this.json=json;this.clock=clock;}

  record Attempt(String paymentId,String attemptId,String sourceService,String businessType,String businessId,
      long businessVersion,String buyerId,long amountMinor,String currency,String description,String returnRoute,
      String customerEmail,String requestHash,String status,String providerSessionId,String providerPaymentIntentId,
      String checkoutUrl,int failureCount,String lastFailureCode,Instant nextReconcileAt,Instant expiresAt,
      Instant createdAt,Instant updatedAt,long aggregateVersion) { }

  @Transactional
  public PaymentContracts.Prepared prepare(PaymentContracts.CreatePayment request) {
    String hash=hash(request);
    var existing=findByAttemptId(request.attemptId());
    if(existing.isPresent()){
      if(!MessageDigest.isEqual(existing.get().requestHash().getBytes(StandardCharsets.US_ASCII),hash.getBytes(StandardCharsets.US_ASCII)))
        throw new Conflict("Idempotency-Key was already used with a different payment request");
      return new PaymentContracts.Prepared(existing.get(),false);
    }
    Instant now=clock.instant(); String id=request.attemptId();
    try {
      db.update("""
        INSERT INTO payment_attempt(payment_id,attempt_id,source_service,business_type,business_id,business_version,buyer_id,
        amount_minor,currency,description,return_route,customer_email,request_hash,status,next_reconcile_at,expires_at,created_at,updated_at,aggregate_version)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,'CREATING',?,?,?,?,1)
        """,id,id,request.sourceService(),request.businessType(),request.businessId(),request.businessVersion(),request.buyerId(),
          request.amountMinor(),request.currency(),request.description(),request.returnRoute(),request.customerEmail(),hash,
          Timestamp.from(now),Timestamp.from(now.plus(Duration.ofHours(24))),Timestamp.from(now),Timestamp.from(now));
    } catch(DuplicateKeyException race) {
      Attempt winner=findByAttemptId(request.attemptId()).orElseThrow(()->race);
      if(!winner.requestHash().equals(hash))throw new Conflict("Idempotency-Key was already used with a different payment request");
      return new PaymentContracts.Prepared(winner,false);
    }
    return new PaymentContracts.Prepared(findByAttemptId(id).orElseThrow(),true);
  }

  @Transactional
  public Attempt ready(String paymentId,PaymentContracts.ProviderResult result) {
    Attempt current=require(paymentId); if(!Set.of("CREATING","RECONCILE_REQUIRED").contains(current.status()))return current;
    Instant now=clock.instant();
    db.update("""
      UPDATE payment_attempt SET provider_session_id=?,provider_payment_intent_id=?,checkout_url=?,status='CHECKOUT_CREATED',
      last_failure_code=NULL,next_reconcile_at=?,updated_at=?,aggregate_version=aggregate_version+1 WHERE payment_id=?
      """,result.sessionId(),result.paymentIntentId(),result.checkoutUrl(),Timestamp.from(now.plusSeconds(60)),Timestamp.from(now),paymentId);
    processPending(result.sessionId()); return require(paymentId);
  }

  @Transactional
  public Attempt retryLater(String paymentId,String failure) {
    Attempt a=require(paymentId); if(!Set.of("CREATING","RECONCILE_REQUIRED").contains(a.status()))return a;
    int count=a.failureCount()+1; Instant now=clock.instant(); long delay=Math.min(300,1L<<Math.min(count,8));
    db.update("UPDATE payment_attempt SET status='RECONCILE_REQUIRED',failure_count=?,last_failure_code=?,next_reconcile_at=?,updated_at=?,aggregate_version=aggregate_version+1 WHERE payment_id=?",
        count,safe(failure),Timestamp.from(now.plusSeconds(delay)),Timestamp.from(now),paymentId);
    return require(paymentId);
  }

  @Transactional
  public Attempt requestExpiry(String paymentId) {
    Attempt a=require(paymentId); if(Set.of("SUCCEEDED","FAILED","EXPIRED").contains(a.status()))return a;
    Instant now=clock.instant();
    db.update("UPDATE payment_attempt SET status='EXPIRY_REQUESTED',next_reconcile_at=?,updated_at=?,aggregate_version=aggregate_version+1 WHERE payment_id=?",Timestamp.from(now),Timestamp.from(now),paymentId);
    return require(paymentId);
  }

  @Transactional
  public void receipt(PaymentContracts.ProviderEvent provider) {
    Instant now=clock.instant();
    try { db.update("""
      INSERT INTO payment_webhook_receipt(provider_event_id,event_type,provider_session_id,provider_payment_intent_id,payment_status,payload_hash,status,received_at,updated_at)
      VALUES (?,?,?,?,?,?,'RECEIVED',?,?)
      """,provider.eventId(),provider.eventType(),provider.sessionId(),provider.paymentIntentId(),provider.paymentStatus(),provider.payloadHash(),Timestamp.from(now),Timestamp.from(now)); }
    catch(DuplicateKeyException duplicate) {
      db.update("UPDATE payment_webhook_receipt SET delivery_count=delivery_count+1,updated_at=? WHERE provider_event_id=?",Timestamp.from(now),provider.eventId()); return;
    }
    applyReceipt(provider.eventId());
  }

  private void processPending(String sessionId) {
    db.queryForList("SELECT provider_event_id FROM payment_webhook_receipt WHERE provider_session_id=? AND status='UNMATCHED' ORDER BY receipt_id",String.class,sessionId)
        .forEach(this::applyReceipt);
  }

  private void applyReceipt(String eventId) {
    Map<String,Object> r=db.queryForMap("SELECT * FROM payment_webhook_receipt WHERE provider_event_id=?",eventId);
    String session=(String)r.get("provider_session_id"); String intent=(String)r.get("provider_payment_intent_id");
    Optional<Attempt> match=session==null?Optional.empty():findBySession(session);
    if(match.isEmpty()&&intent!=null)match=findByIntent(intent);
    Instant now=clock.instant();
    if(match.isEmpty()){db.update("UPDATE payment_webhook_receipt SET status='UNMATCHED',updated_at=? WHERE provider_event_id=?",Timestamp.from(now),eventId);return;}
    Attempt a=match.get(); String type=(String)r.get("event_type"); String status=null; String routing=null;
    if(("checkout.session.completed".equals(type)||"checkout.session.async_payment_succeeded".equals(type))&&"paid".equals(r.get("payment_status"))){status="SUCCEEDED";routing="payment.succeeded.v1";}
    else if("checkout.session.expired".equals(type)){status="EXPIRED";routing="payment.expired.v1";}
    else if("payment_intent.payment_failed".equals(type)||"checkout.session.async_payment_failed".equals(type)){status="FAILED";routing="payment.failed.v1";}
    if(status==null){db.update("UPDATE payment_webhook_receipt SET status='IGNORED',processed_at=?,updated_at=? WHERE provider_event_id=?",Timestamp.from(now),Timestamp.from(now),eventId);return;}
    if("SUCCEEDED".equals(a.status())&&!"SUCCEEDED".equals(status)){
      db.update("UPDATE payment_webhook_receipt SET status='IGNORED',processed_at=?,updated_at=? WHERE provider_event_id=?",Timestamp.from(now),Timestamp.from(now),eventId);return;
    }
    if(!a.status().equals(status)){
      db.update("UPDATE payment_attempt SET status=?,provider_payment_intent_id=COALESCE(provider_payment_intent_id,?),next_reconcile_at=NULL,updated_at=?,aggregate_version=aggregate_version+1 WHERE payment_id=?",
          status,intent,Timestamp.from(now),a.paymentId());
      event(require(a.paymentId()),routing,eventId);
    }
    db.update("UPDATE payment_webhook_receipt SET status='PROCESSED',processed_at=?,updated_at=? WHERE provider_event_id=?",Timestamp.from(now),Timestamp.from(now),eventId);
  }

  @Transactional
  public Attempt applyProviderState(String paymentId,PaymentContracts.ProviderState state) {
    Attempt a=require(paymentId); if(Set.of("SUCCEEDED","FAILED","EXPIRED").contains(a.status()))return a;
    String status=null,routing=null;
    if("paid".equals(state.paymentStatus())){status="SUCCEEDED";routing="payment.succeeded.v1";}
    else if("expired".equals(state.sessionStatus())){status="EXPIRED";routing="payment.expired.v1";}
    Instant now=clock.instant();
    if(status==null){
      db.update("UPDATE payment_attempt SET provider_payment_intent_id=COALESCE(provider_payment_intent_id,?),next_reconcile_at=?,updated_at=? WHERE payment_id=?",
          state.paymentIntentId(),Timestamp.from(now.plusSeconds(60)),Timestamp.from(now),paymentId);
      return require(paymentId);
    }
    db.update("UPDATE payment_attempt SET status=?,provider_payment_intent_id=COALESCE(provider_payment_intent_id,?),next_reconcile_at=NULL,updated_at=?,aggregate_version=aggregate_version+1 WHERE payment_id=?",
        status,state.paymentIntentId(),Timestamp.from(now),paymentId);
    Attempt updated=require(paymentId);event(updated,routing,"provider-reconciliation");return updated;
  }

  void deferProviderCheck(String paymentId) {
    Instant now=clock.instant();
    db.update("UPDATE payment_attempt SET next_reconcile_at=?,updated_at=? WHERE payment_id=?",Timestamp.from(now.plusSeconds(60)),Timestamp.from(now),paymentId);
  }

  private void event(Attempt a,String type,String causation) {
    try {
      String eventId=UUID.randomUUID().toString(); Instant now=clock.instant();
      Map<String,Object> payload=new LinkedHashMap<>();
      payload.put("eventId",eventId);payload.put("eventType",type);payload.put("schemaVersion",1);payload.put("occurredAt",now.toString());
      payload.put("producer","payment-service");payload.put("aggregateType","PaymentAttempt");payload.put("aggregateId",a.paymentId());
      payload.put("aggregateVersion",a.aggregateVersion());payload.put("correlationId",a.attemptId());payload.put("causationId",causation);
      payload.put("payload",Map.ofEntries(Map.entry("paymentId",a.paymentId()),Map.entry("attemptId",a.attemptId()),
          Map.entry("sourceService",a.sourceService()),Map.entry("businessType",a.businessType()),Map.entry("businessId",a.businessId()),
          Map.entry("businessVersion",a.businessVersion()),Map.entry("buyerId",a.buyerId()),Map.entry("amountMinor",a.amountMinor()),
          Map.entry("currency",a.currency()),Map.entry("status",a.status()),Map.entry("providerPaymentIntentId",Objects.toString(a.providerPaymentIntentId(),""))));
      db.update("INSERT INTO payment_outbox(event_id,aggregate_id,aggregate_version,routing_key,payload_json,correlation_id,causation_id,status,created_at) VALUES (?,?,?,?,?,?,?,'PENDING',?)",
          eventId,a.paymentId(),a.aggregateVersion(),type,json.writeValueAsString(payload),a.attemptId(),causation,Timestamp.from(now));
    }catch(Exception e){throw new IllegalStateException("Could not persist payment result",e);}
  }

  Optional<Attempt> findByAttemptId(String id){return one("SELECT * FROM payment_attempt WHERE attempt_id=?",id);}
  Optional<Attempt> findByPaymentId(String id){return one("SELECT * FROM payment_attempt WHERE payment_id=?",id);}
  Optional<Attempt> findBySession(String id){return one("SELECT * FROM payment_attempt WHERE provider_session_id=?",id);}
  Optional<Attempt> findByIntent(String id){return one("SELECT * FROM payment_attempt WHERE provider_payment_intent_id=?",id);}
  List<String> dueCreation(int limit){return db.queryForList("SELECT payment_id FROM payment_attempt WHERE status IN ('CREATING','RECONCILE_REQUIRED') AND next_reconcile_at<=? ORDER BY created_at LIMIT ?",String.class,Timestamp.from(clock.instant()),limit);}
  List<String> dueProviderCheck(int limit){return db.queryForList("SELECT payment_id FROM payment_attempt WHERE status IN ('CHECKOUT_CREATED','EXPIRY_REQUESTED') AND next_reconcile_at<=? ORDER BY updated_at LIMIT ?",String.class,Timestamp.from(clock.instant()),limit);}
  @Transactional public void processUnmatched(int limit){db.queryForList("SELECT provider_event_id FROM payment_webhook_receipt WHERE status='UNMATCHED' ORDER BY received_at LIMIT ?",String.class,limit).forEach(this::applyReceipt);}
  Attempt require(String id){return findByPaymentId(id).orElseThrow(NotFound::new);}
  private Optional<Attempt> one(String sql,String value){List<Attempt> rows=db.query(sql,(rs,n)->new Attempt(rs.getString("payment_id"),rs.getString("attempt_id"),rs.getString("source_service"),rs.getString("business_type"),rs.getString("business_id"),rs.getLong("business_version"),rs.getString("buyer_id"),rs.getLong("amount_minor"),rs.getString("currency"),rs.getString("description"),rs.getString("return_route"),rs.getString("customer_email"),rs.getString("request_hash"),rs.getString("status"),rs.getString("provider_session_id"),rs.getString("provider_payment_intent_id"),rs.getString("checkout_url"),rs.getInt("failure_count"),rs.getString("last_failure_code"),instant(rs.getTimestamp("next_reconcile_at")),instant(rs.getTimestamp("expires_at")),instant(rs.getTimestamp("created_at")),instant(rs.getTimestamp("updated_at")),rs.getLong("aggregate_version")),value);return rows.stream().findFirst();}
  private static Instant instant(Timestamp value){return value==null?null:value.toInstant();}
  private String hash(PaymentContracts.CreatePayment r){try{return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(r)));}catch(Exception e){throw new IllegalStateException(e);}}
  private static String safe(String value){if(value==null||value.isBlank())return "PROVIDER_ERROR";return value.substring(0,Math.min(80,value.length()));}
  static final class Conflict extends RuntimeException {Conflict(String m){super(m);}}
  static final class NotFound extends RuntimeException { }
}
