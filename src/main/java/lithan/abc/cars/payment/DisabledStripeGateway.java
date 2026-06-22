package lithan.abc.cars.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lithan.abc.cars.entity.PaymentOrder;
import lithan.abc.cars.entity.StoreOrder;
import lithan.abc.cars.entity.UserAccount;

@Component
@ConditionalOnProperty(name = "payments.stripe.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledStripeGateway implements StripeGateway {

  private PaymentProviderException disabled() {
    return new PaymentProviderException("Stripe payments are not configured");
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

  @Override
  public String createConnectedAccount(UserAccount seller) {
    throw disabled();
  }

  @Override
  public String createOnboardingLink(String accountId) {
    throw disabled();
  }

  @Override
  public StripeAccountState retrieveAccountState(String accountId) {
    throw disabled();
  }

  @Override
  public StripeCheckoutResult createCheckoutSession(PaymentOrder paymentOrder, String destinationAccountId) {
    throw disabled();
  }

  @Override
  public StripeCheckoutResult createStoreCheckoutSession(StoreOrder order) {
    throw disabled();
  }

  @Override
  public StripeWebhookEvent verifyAndParseWebhook(String payload, String signature) {
    throw disabled();
  }
}
