package lithan.autostrada.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Offline copy/cutover utility. It requires an explicit write freeze and distinct schemas. */
public final class PaymentCopy {
  private PaymentCopy(){}
  public static void main(String[] args){
    if(!"I_HAVE_STOPPED_ALL_PAYMENT_WRITERS".equals(System.getenv("CUTOVER_WRITE_FREEZE"))||!"UTC".equals(System.getenv("CUTOVER_SOURCE_TIMEZONE")))throw new IllegalStateException("A verified UTC source and stopped payment writers are required");
    try(var source=DriverManager.getConnection(env("CUTOVER_SOURCE_URL"),env("CUTOVER_SOURCE_USER"),env("CUTOVER_SOURCE_PASSWORD"));var target=DriverManager.getConnection(env("CUTOVER_TARGET_URL"),env("CUTOVER_TARGET_USER"),env("CUTOVER_TARGET_PASSWORD"))){
      if(source.getMetaData().getURL().equals(target.getMetaData().getURL()))throw new IllegalStateException("Distinct owner schemas required");copy(source,target);
    }catch(Exception failure){System.err.println("Payment copy failed; preserve the write freeze and private diagnostics.");System.exit(1);}
  }
  static void copy(Connection source,Connection target)throws Exception{
    var attempts=attempts(source);var accounts=rows(source,"SELECT id_payment_account,id_user,provider_account_id,status,transfers_enabled,created_at,updated_at FROM tb_payment_account ORDER BY id_payment_account",7);
    var legacy=rows(source,"SELECT id_payment,id_bid,id_buyer,id_seller,amount_minor,platform_fee_minor,currency,status,purpose,checkout_session_id,payment_intent_id,created_at,updated_at,paid_at,version FROM tb_payment_order ORDER BY id_payment",15);
    var receipts=receipts(source);target.setAutoCommit(false);
    try{
      requireEmpty(target,"payment_attempt");requireEmpty(target,"payment_provider_account_audit");requireEmpty(target,"payment_legacy_audit");requireEmpty(target,"payment_webhook_receipt");
      insert(target,"INSERT INTO payment_attempt(payment_id,attempt_id,source_service,business_type,business_id,business_version,buyer_id,amount_minor,currency,description,return_route,customer_email,request_hash,status,provider_session_id,provider_payment_intent_id,checkout_url,failure_count,last_failure_code,next_reconcile_at,expires_at,created_at,updated_at,aggregate_version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",attempts);
      insert(target,"INSERT INTO payment_provider_account_audit(original_id,user_id,provider_account_id,status,transfers_enabled,created_at,updated_at) VALUES (?,?,?,?,?,?,?)",accounts);
      insert(target,"INSERT INTO payment_legacy_audit(original_id,bid_id,buyer_id,seller_id,amount_minor,platform_fee_minor,currency,status,purpose,provider_session_id,provider_payment_intent_id,created_at,updated_at,paid_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",legacy);
      insert(target,"INSERT INTO payment_webhook_receipt(provider_event_id,event_type,provider_session_id,provider_payment_intent_id,payment_status,payload_hash,status,delivery_count,received_at,updated_at,processed_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)",receipts);
      verify(target,"payment_attempt",attempts);verify(target,"payment_provider_account_audit",accounts);verify(target,"payment_legacy_audit",legacy);
      if(count(target,"payment_webhook_receipt")!=receipts.size())throw new SQLException("Receipt count parity mismatch");
      try(var s=target.prepareStatement("UPDATE payment_copy_checkpoint SET state='PARITY_VERIFIED',source_fingerprint=?,verified_at=?,updated_at=? WHERE id=1")){var now=Timestamp.from(Instant.now());s.setString(1,fingerprint(attempts,accounts,legacy,receipts));s.setTimestamp(2,now);s.setTimestamp(3,now);s.executeUpdate();}
      target.commit();
    }catch(Exception error){target.rollback();throw error;}
    if(!attempts.equals(attempts(source))||!accounts.equals(rows(source,"SELECT id_payment_account,id_user,provider_account_id,status,transfers_enabled,created_at,updated_at FROM tb_payment_account ORDER BY id_payment_account",7))||!legacy.equals(rows(source,"SELECT id_payment,id_bid,id_buyer,id_seller,amount_minor,platform_fee_minor,currency,status,purpose,checkout_session_id,payment_intent_id,created_at,updated_at,paid_at,version FROM tb_payment_order ORDER BY id_payment",15)))throw new SQLException("Source changed during payment copy");
    try(var s=source.prepareStatement("UPDATE payment_cutover SET state='PARITY_VERIFIED',verified_at=CURRENT_TIMESTAMP WHERE id=1")){if(s.executeUpdate()!=1)throw new SQLException("Missing payment cutover marker");}
    System.out.println("Payment copy parity verified: attempts, amounts, provider IDs, legacy audits, accounts and receipts.");
  }
  private static List<List<Object>> attempts(Connection c)throws Exception{
    var out=new ArrayList<List<Object>>();var json=new ObjectMapper();String sql="""
      SELECT a.*,o.id_order,o.total_minor AS order_amount,o.currency AS order_currency,d.id_deposit,d.amount_minor AS deposit_amount,d.currency AS deposit_currency,d.id_buyer
      FROM tb_checkout_attempt a LEFT JOIN tb_store_order o ON o.checkout_attempt_id=a.attempt_id LEFT JOIN tb_listing_deposit d ON d.checkout_attempt_id=a.attempt_id ORDER BY a.attempt_id
      """;
    try(var s=c.createStatement();var r=s.executeQuery(sql)){while(r.next()){
      boolean store="STORE_ORDER".equals(r.getString("purpose"));String source=store?"commerce-service":"marketplace-service";String business=store?"STORE_ORDER":"LISTING_DEPOSIT";
      String businessId=Integer.toString(r.getInt(store?"id_order":"id_deposit"));String buyer=Integer.toString(store?r.getInt("id_user"):r.getInt("id_buyer"));long amount=r.getLong(store?"order_amount":"deposit_amount");String currency=r.getString(store?"order_currency":"deposit_currency");String description=(store?"Order ":"Listing deposit ")+businessId;
      var request=new PaymentContracts.CreatePayment(r.getString("attempt_id"),source,business,businessId,1,buyer,amount,currency,description,business,r.getString("customer_email"));
      String status=switch(r.getString("status")){case "PAID","PAID_STOCK_CONFLICT","PAID_RESERVATION_CONFLICT"->"SUCCEEDED";case "PAYMENT_FAILED"->"FAILED";default->r.getString("status");};
      out.add(Arrays.asList(r.getString("attempt_id"),r.getString("attempt_id"),source,business,businessId,1L,buyer,amount,currency,description,business,r.getString("customer_email"),sha(json.writeValueAsBytes(request)),status,r.getString("provider_session_id"),r.getString("payment_intent_id"),r.getString("provider_checkout_url"),r.getInt("failure_count"),r.getString("last_failure_code"),r.getTimestamp("next_reconcile_at"),r.getTimestamp("expires_at"),r.getTimestamp("created_at"),r.getTimestamp("updated_at"),Math.max(1,r.getLong("version")+1)));
    }}return out;
  }
  private static List<List<Object>> receipts(Connection c)throws Exception{
    var out=new ArrayList<List<Object>>();Set<String> ids=new HashSet<>();
    try(var s=c.createStatement();var r=s.executeQuery("SELECT provider_event_id,event_type,checkout_session_id,payment_intent_id,payment_status,status,delivery_count,received_at,updated_at,processed_at FROM tb_checkout_webhook_inbox ORDER BY id_inbox")){while(r.next()){
      String id=r.getString(1);if(!ids.add(id))throw new SQLException("Duplicate provider event across sources");out.add(Arrays.asList(id,r.getString(2),r.getString(3),r.getString(4),r.getString(5),sha(("checkout:"+id).getBytes(StandardCharsets.UTF_8)),"PROCESSED".equals(r.getString(6))?"PROCESSED":"UNMATCHED",r.getInt(7),r.getTimestamp(8),r.getTimestamp(9),r.getTimestamp(10)));
    }}
    try(var s=c.createStatement();var r=s.executeQuery("SELECT provider_event_id,event_type,processed_at FROM tb_payment_webhook_event ORDER BY id_webhook_event")){while(r.next()){
      String id=r.getString(1);if(!ids.add(id))throw new SQLException("Duplicate provider event across sources");Timestamp time=r.getTimestamp(3);out.add(Arrays.asList(id,r.getString(2),null,null,null,sha(("legacy:"+id).getBytes(StandardCharsets.UTF_8)),"IGNORED",1,time,time,time));
    }}return out;
  }
  private static List<List<Object>> rows(Connection c,String sql,int columns)throws Exception{var out=new ArrayList<List<Object>>();try(var s=c.createStatement();var r=s.executeQuery(sql)){while(r.next()){var row=new ArrayList<>();for(int i=1;i<=columns;i++)row.add(r.getObject(i));out.add(row);}}return out;}
  private static void insert(Connection c,String sql,List<List<Object>> rows)throws Exception{try(var p=c.prepareStatement(sql)){for(var row:rows){for(int i=0;i<row.size();i++)p.setObject(i+1,row.get(i));p.addBatch();}p.executeBatch();}}
  private static void verify(Connection c,String table,List<List<Object>> expected)throws Exception{if(count(c,table)!=expected.size())throw new SQLException(table+" count parity mismatch");}
  private static long count(Connection c,String table)throws Exception{try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getLong(1);}}
  private static void requireEmpty(Connection c,String table)throws Exception{if(count(c,table)!=0)throw new SQLException("Target "+table+" is not empty");}
  private static String fingerprint(List<?>... values)throws Exception{return sha(new ObjectMapper().writeValueAsBytes(Arrays.asList(values)));}
  private static String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
  private static String env(String key){String value=System.getenv(key);if(value==null||value.isBlank())throw new IllegalArgumentException("Missing "+key);return value;}
}
