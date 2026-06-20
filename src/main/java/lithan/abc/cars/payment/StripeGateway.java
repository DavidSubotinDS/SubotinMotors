package lithan.abc.cars.payment;

import lithan.abc.cars.entity.PaymentOrder;
import lithan.abc.cars.entity.UserAccount;

public interface StripeGateway {
  boolean isEnabled();

  String createConnectedAccount(UserAccount seller);

  String createOnboardingLink(String accountId);

  StripeAccountState retrieveAccountState(String accountId);

  StripeCheckoutResult createCheckoutSession(PaymentOrder paymentOrder, String destinationAccountId);

  StripeWebhookEvent verifyAndParseWebhook(String payload, String signature);
}
