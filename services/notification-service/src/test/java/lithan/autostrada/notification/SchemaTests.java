package lithan.autostrada.notification;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SchemaTests {
  final ObjectMapper json=new ObjectMapper();
  final JsonSchema schema=JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
      .getSchema(getClass().getResourceAsStream("/contracts/notification-message-v1.schema.json"));

  @Test void businessFixtureValidatesAndPrivacyAndVersionRegressionsFail() {
    var event=json.valueToTree(InboxTests.event(UUID.randomUUID().toString()));
    assertThat(schema.validate(event)).isEmpty();
    ((com.fasterxml.jackson.databind.node.ObjectNode)event.path("payload").path("auctionSnapshot")).put("email","private@example.test");
    assertThat(schema.validate(event)).isNotEmpty();
    ((com.fasterxml.jackson.databind.node.ObjectNode)event.path("payload").path("auctionSnapshot")).remove("email");
    ((com.fasterxml.jackson.databind.node.ObjectNode)event).put("schemaVersion",2);
    assertThat(schema.validate(event)).isNotEmpty();
  }

  @Test void encryptedCommandFixtureValidatesAndWrongProducerFails() {
    String id=UUID.randomUUID().toString();var e=InboxTests.event(id);
    e.put("eventType",Inbox.RESET);e.put("producer","identity-service");e.put("aggregateType","PasswordResetDelivery");e.put("aggregateId",id);
    e.put("payload",Map.of("deliveryId",id,"expiresAt","2030-01-01T00:30:00Z","encryptedPayload","v1.fixture.fixture"));
    assertThat(schema.validate(json.valueToTree(e))).isEmpty();
    e.put("producer","legacy-backend");assertThat(schema.validate(json.valueToTree(e))).isNotEmpty();
  }
}
