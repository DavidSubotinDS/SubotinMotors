package lithan.autostrada.notification;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;

class InboxTests {
  JdbcTemplate db;Inbox inbox;ObjectMapper json=new ObjectMapper();
  static final Instant NOW=Instant.parse("2030-01-01T00:00:00Z");
  @BeforeEach void setup(){
    var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
    org.flywaydb.core.Flyway.configure().dataSource(ds).load().migrate();db=new JdbcTemplate(ds);
    inbox=new Inbox(db,json,new TransactionTemplate(new DataSourceTransactionManager(ds)),Clock.fixed(NOW,ZoneOffset.UTC));
  }
  static Map<String,Object> event(String id) {
    var snapshot=new LinkedHashMap<String,Object>();snapshot.put("id",10);snapshot.put("make","Test");snapshot.put("model","Car");snapshot.put("year","2024");
    snapshot.put("price",100);snapshot.put("status","ENDING_SOON");snapshot.put("statusLabel","Ending soon");snapshot.put("auctionEndTime","2030-01-02T00:00:00");
    snapshot.put("auctionEndTimeEpochMillis",1893542400000L);snapshot.put("imageUrl",null);snapshot.put("imageUrls",List.of());snapshot.put("sellerDisplayName",null);
    var event=new LinkedHashMap<String,Object>();event.put("eventId",id);event.put("eventType",Inbox.ENDING);event.put("schemaVersion",1);event.put("occurredAt",NOW.toString());
    event.put("producer","legacy-backend");event.put("aggregateType","AuctionNotification");event.put("aggregateId","2:10:ENDING_SOON");event.put("aggregateVersion",1);
    event.put("correlationId",UUID.randomUUID().toString());event.put("causationId",null);
    event.put("payload",Map.of("recipientId","2","auctionId","10","notificationType","AUCTION_ENDING_SOON","message","Test Car is ending soon.","dedupeKey","2:10:ENDING_SOON","auctionSnapshot",snapshot));return event;
  }
  @Test void eventAndBusinessDedupePreserveReadState() throws Exception {
    var first=json.writeValueAsBytes(event(UUID.randomUUID().toString()));inbox.accept(first,false);inbox.accept(first,false);
    int id=((Number)inbox.list(2).get(0).get("idNotification")).intValue();
    assertThat(inbox.list(3)).isEmpty();assertThat(inbox.unread(2)).isEqualTo(1);
    assertThatThrownBy(()->inbox.read(3,id)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    inbox.read(2,id);Object read=inbox.list(2).get(0).get("readAt");
    inbox.accept(json.writeValueAsBytes(event(UUID.randomUUID().toString())),false);inbox.readAll(2);
    assertThat(inbox.list(2)).hasSize(1);assertThat(inbox.unread(2)).isZero();assertThat(inbox.list(2).get(0).get("readAt")).isEqualTo(read);
    assertThat(db.queryForObject("SELECT COUNT(*) FROM tb_message_inbox",Long.class)).isEqualTo(2);
  }
  @Test void rejectedContractDoesNotLeaveDedupeMarker() throws Exception {
    var event=event(UUID.randomUUID().toString());event.put("schemaVersion",2);
    assertThatThrownBy(()->inbox.accept(json.writeValueAsBytes(event),false)).isInstanceOf(Inbox.InvalidMessage.class);
    assertThat(db.queryForObject("SELECT COUNT(*) FROM tb_message_inbox",Long.class)).isZero();
    event.put("schemaVersion",1);inbox.accept(json.writeValueAsBytes(event),false);assertThat(inbox.unread(2)).isEqualTo(1);
  }
  @Test void rollbackDoesNotConsumeEvent() throws Exception {
    db.execute("ALTER TABLE tb_notification ADD CONSTRAINT test_poison CHECK (id_user<>2)");
    byte[] event=json.writeValueAsBytes(event(UUID.randomUUID().toString()));
    assertThatThrownBy(()->inbox.accept(event,false)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(db.queryForObject("SELECT COUNT(*) FROM tb_message_inbox",Long.class)).isZero();
    db.execute("ALTER TABLE tb_notification DROP CONSTRAINT test_poison");inbox.accept(event,false);assertThat(inbox.unread(2)).isEqualTo(1);
  }
  @Test void encryptionIsRandomizedAuthenticatedAndContextBound() {
    var cipher=new DeliveryCipher(Base64.getEncoder().encodeToString(new byte[32]));
    String a=cipher.encrypt("private reset payload","id|expiry"),b=cipher.encrypt("private reset payload","id|expiry");
    assertThat(a).isNotEqualTo(b).doesNotContain("private");assertThat(cipher.decrypt(a,"id|expiry")).isEqualTo("private reset payload");
    assertThatThrownBy(()->cipher.decrypt(a,"other|expiry")).isInstanceOf(Inbox.InvalidMessage.class);
  }
  @Test void expiredCommandCannotPersistSensitivePayload() throws Exception {
    String id=UUID.randomUUID().toString();var e=event(id);e.put("eventType",Inbox.RESET);e.put("producer","identity-service");e.put("aggregateType","PasswordResetDelivery");e.put("aggregateId",id);
    e.put("payload",Map.of("deliveryId",id,"expiresAt",NOW.minusSeconds(1).toString(),"encryptedPayload","v1.encrypted.secret"));
    inbox.accept(json.writeValueAsBytes(e),true);
    assertThat(db.queryForObject("SELECT state FROM tb_delivery",String.class)).isEqualTo("EXPIRED");
    assertThat(db.queryForObject("SELECT encrypted_payload FROM tb_delivery",String.class)).isNull();
  }

  @Test void deliveryErasesPayloadAndDuplicateCannotSendAgain() throws Exception {
    var cipher=new DeliveryCipher(Base64.getEncoder().encodeToString(new byte[32]));
    String id=UUID.randomUUID().toString();byte[] event=delivery(id,NOW.plusSeconds(60),cipher);
    inbox.accept(event,true);var sent=new java.util.concurrent.atomic.AtomicInteger();
    var worker=worker(cipher,NOW,sent);worker.deliver();inbox.accept(event,true);worker.deliver();
    assertThat(sent.get()).isEqualTo(1);
    assertThat(db.queryForObject("SELECT encrypted_payload FROM tb_delivery",String.class)).isNull();
  }

  @Test void expiredPendingDeliveryIsPurgedWithoutSending() throws Exception {
    var cipher=new DeliveryCipher(Base64.getEncoder().encodeToString(new byte[32]));
    inbox.accept(delivery(UUID.randomUUID().toString(),NOW.plusSeconds(1),cipher),true);
    var sent=new java.util.concurrent.atomic.AtomicInteger();worker(cipher,NOW.plusSeconds(2),sent).deliver();
    assertThat(sent.get()).isZero();assertThat(db.queryForObject("SELECT state FROM tb_delivery",String.class)).isEqualTo("EXPIRED");
    assertThat(db.queryForObject("SELECT encrypted_payload FROM tb_delivery",String.class)).isNull();
  }

  @Test void tamperedCiphertextNeverReachesMailTransport() throws Exception {
    var cipher=new DeliveryCipher(Base64.getEncoder().encodeToString(new byte[32]));
    inbox.accept(delivery(UUID.randomUUID().toString(),NOW.plusSeconds(60),cipher),true);
    db.update("UPDATE tb_delivery SET encrypted_payload='v1.invalid.invalid'");
    var sent=new java.util.concurrent.atomic.AtomicInteger();worker(cipher,NOW,sent).deliver();
    assertThat(sent.get()).isZero();assertThat(db.queryForObject("SELECT state FROM tb_delivery",String.class)).isEqualTo("FAILED");
    worker(cipher,NOW.plusSeconds(61),sent).deliver();
    assertThat(db.queryForObject("SELECT encrypted_payload FROM tb_delivery",String.class)).isNull();
  }

  private byte[] delivery(String id,Instant expiry,DeliveryCipher cipher) throws Exception {
    var e=event(id);e.put("eventType",Inbox.RESET);e.put("producer","identity-service");e.put("aggregateType","PasswordResetDelivery");e.put("aggregateId",id);
    String plain=json.writeValueAsString(Map.of("deliveryId",id,"expiresAt",expiry.toString(),"template","password-reset-v1",
        "recipientEmail","fixture@example.test","resetUrl","http://localhost:8081/reset-password?token=private-fixture"));
    e.put("payload",Map.of("deliveryId",id,"expiresAt",expiry.toString(),"encryptedPayload",cipher.encrypt(plain,id+"|"+expiry)));
    return json.writeValueAsBytes(e);
  }

  private DeliveryWorker worker(DeliveryCipher cipher,Instant time,java.util.concurrent.atomic.AtomicInteger sent) {
    var provider=new org.springframework.beans.factory.support.DefaultListableBeanFactory().getBeanProvider(org.springframework.mail.javamail.JavaMailSender.class);
    return new DeliveryWorker(db,cipher,json,Clock.fixed(time,ZoneOffset.UTC),provider,"smtp","fixture@example.test",new io.micrometer.core.instrument.simple.SimpleMeterRegistry()) {
      @Override protected void deliverMessage(String to,String subject,String body) {sent.incrementAndGet();}
    };
  }
}
