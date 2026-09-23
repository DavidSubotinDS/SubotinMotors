package lithan.autostrada.identity;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;
import lithan.autostrada.identity.service.*;

@SpringBootTest
class DeliveryOutboxTests extends IdentityTestBase {
  @Autowired TransactionTemplate tx;
  @Autowired QueuedEmailService deliveries;
  @Autowired DeliveryCipher cipher;
  @Test void resetCommandIsEncryptedAndRollsBackWithItsOwnerTransaction() {
    long before=fixtureSql.queryForObject("SELECT COUNT(*) FROM tb_delivery_outbox",Long.class);
    tx.executeWithoutResult(status->{deliveries.sendReset("private@example.invalid","http://localhost/reset-password?token=private-token",Instant.now().plusSeconds(300));status.setRollbackOnly();});
    assertThat(fixtureSql.queryForObject("SELECT COUNT(*) FROM tb_delivery_outbox",Long.class)).isEqualTo(before);
    tx.executeWithoutResult(status->deliveries.sendReset("private@example.invalid","http://localhost/reset-password?token=private-token",Instant.now().plusSeconds(300)));
    var row=fixtureSql.queryForList("SELECT event_id,encrypted_payload,expires_at FROM tb_delivery_outbox ORDER BY created_at DESC LIMIT 1").get(0);
    String encrypted=(String)row.get("encrypted_payload");assertThat(encrypted).doesNotContain("private","token","example.invalid");
    String context=row.get("event_id")+"|"+((java.sql.Timestamp)row.get("expires_at")).toInstant();
    assertThat(cipher.decrypt(encrypted,context)).contains("password-reset-v1","private-token");
    fixtureSql.update("DELETE FROM tb_delivery_outbox WHERE event_id=?",row.get("event_id"));
  }
}
