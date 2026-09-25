package lithan.autostrada.auctions;

import static org.assertj.core.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import lithan.autostrada.auctions.payment.PaymentResultInbox;

@org.springframework.context.annotation.Import(BusinessIdentityFixtures.class)
@SpringBootTest
@Transactional
class PaymentResultInboxIntegrationTests {
  @Autowired JdbcTemplate db;@Autowired PaymentResultInbox inbox;
  @Autowired jakarta.persistence.EntityManager entities;
  @Test void normalizedResultAppliesBusinessStateExactlyOnce(){
    String attempt="ad7343cb-c784-45cb-b9d0-4a5428bdf881";
    db.update("INSERT INTO tb_checkout_attempt(attempt_id,id_user,purpose,client_request_id,request_hash,customer_email,status,failure_count,expires_at,created_at,updated_at,version) VALUES (?,1,'STORE_ORDER','result-test',?,'buyer@example.test','CHECKOUT_CREATED',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)",attempt,"a".repeat(64));
    db.update("INSERT INTO tb_store_order(id_user,total_minor,currency,status,shipping_name,shipping_address,shipping_street_address,shipping_city,shipping_postal_code,shipping_country,created_at,updated_at,version,checkout_attempt_id) VALUES (1,19900,'eur','CHECKOUT_CREATED','Buyer','Street','Street','City','10000','RS',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0,?)",attempt);
    byte[] event=("{\"eventId\":\"31dd3af3-3ef0-49c1-89b7-00eac0c6e07d\",\"eventType\":\"payment.succeeded.v1\",\"payload\":{\"attemptId\":\""+attempt+"\",\"businessType\":\"STORE_ORDER\",\"status\":\"SUCCEEDED\",\"providerPaymentIntentId\":\"pi_result\"}}").getBytes(StandardCharsets.UTF_8);
    inbox.accept(event);inbox.accept(event);entities.flush();entities.clear();
    assertThat(db.queryForObject("SELECT status FROM tb_store_order WHERE checkout_attempt_id=?",String.class,attempt)).isEqualTo("PAID");
    assertThat(db.queryForObject("SELECT payment_intent_id FROM tb_store_order WHERE checkout_attempt_id=?",String.class,attempt)).isEqualTo("pi_result");
    assertThat(db.queryForObject("SELECT COUNT(*) FROM tb_payment_result_inbox WHERE attempt_id=?",Integer.class,attempt)).isEqualTo(1);
  }
}
