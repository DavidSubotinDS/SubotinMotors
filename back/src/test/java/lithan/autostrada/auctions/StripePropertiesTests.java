package lithan.autostrada.auctions;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import lithan.autostrada.auctions.config.StripeProperties;

class StripePropertiesTests {

  @Test
  void sandboxCredentialsAreAccepted() {
    StripeProperties properties = enabledProperties();
    properties.setSecretKey("sk_test_school_project");

    assertDoesNotThrow(properties::validate);

    properties.setSecretKey("rk_test_school_project");
    assertDoesNotThrow(properties::validate);

    properties.setSecretKey("rkcs_test_school_project");
    assertDoesNotThrow(properties::validate);
  }

  @Test
  void liveCredentialsAreRejected() {
    StripeProperties properties = enabledProperties();
    properties.setSecretKey("sk_live_never_allowed");

    assertThrows(IllegalStateException.class, properties::validate);
  }

  private StripeProperties enabledProperties() {
    StripeProperties properties = new StripeProperties();
    properties.setEnabled(true);
    properties.setWebhookSecret("whsec_school_project");
    return properties;
  }
}
