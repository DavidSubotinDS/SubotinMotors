package lithan.abc.cars.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payments.stripe")
public class StripeProperties {

  private boolean enabled;
  private String secretKey;
  private String webhookSecret;
  private String currency = "eur";
  private int platformFeeBasisPoints = 250;
  private String baseUrl = "http://localhost:8080";

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }

  public String getWebhookSecret() {
    return webhookSecret;
  }

  public void setWebhookSecret(String webhookSecret) {
    this.webhookSecret = webhookSecret;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getPlatformFeeBasisPoints() {
    return platformFeeBasisPoints;
  }

  public void setPlatformFeeBasisPoints(int platformFeeBasisPoints) {
    this.platformFeeBasisPoints = platformFeeBasisPoints;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public void validate() {
    if (!enabled) {
      return;
    }
    if (secretKey == null || secretKey.isBlank()) {
      throw new IllegalStateException("STRIPE_SECRET_KEY is required when Stripe is enabled");
    }
    if (!secretKey.startsWith("sk_test_")
        && !secretKey.startsWith("rk_test_")
        && !secretKey.startsWith("rkcs_test_")) {
      throw new IllegalStateException(
          "This educational project accepts only Stripe sandbox credentials");
    }
    if (webhookSecret == null || webhookSecret.isBlank()) {
      throw new IllegalStateException("STRIPE_WEBHOOK_SECRET is required when Stripe is enabled");
    }
    if (!webhookSecret.startsWith("whsec_")) {
      throw new IllegalStateException("STRIPE_WEBHOOK_SECRET must be a Stripe whsec_ signing secret");
    }
    if (platformFeeBasisPoints < 0 || platformFeeBasisPoints > 10000) {
      throw new IllegalStateException("Stripe platform fee basis points must be between 0 and 10000");
    }
  }
}
