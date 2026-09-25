package lithan.autostrada.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="payment.provider.enabled",havingValue="false",matchIfMissing=true)
class DisabledPaymentProvider implements PaymentProvider {
  public boolean enabled(){return false;}
  public PaymentContracts.ProviderResult create(PaymentStore.Attempt attempt){throw new IllegalStateException("Payment provider is disabled");}
  public PaymentContracts.ProviderEvent verify(String payload,String signature){throw new IllegalArgumentException("Payment provider is disabled");}
}
