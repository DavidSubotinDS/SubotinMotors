package lithan.autostrada.auctions.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Business defaults retained by the legacy commerce/marketplace modules. */
@Configuration
@ConfigurationProperties(prefix="payment.policy")
public class PaymentPolicyProperties {
  private String currency="eur";
  public String getCurrency(){return currency;}
  public void setCurrency(String value){currency=value;}
}
