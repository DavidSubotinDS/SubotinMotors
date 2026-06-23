package lithan.abc.cars.payment;

import lithan.abc.cars.entity.PaymentOrder;
import lithan.abc.cars.entity.ListingDeposit;
import lithan.abc.cars.entity.StoreOrder;
import lithan.abc.cars.entity.UserAccount;

public interface StripeGateway {
  boolean isEnabled();

  String createConnectedAccount(UserAccount seller);

  String createOnboardingLink(String accountId);

  StripeAccountState retrieveAccountState(String accountId);

  StripeCheckoutResult createCheckoutSession(PaymentOrder paymentOrder, String destinationAccountId);

  StripeCheckoutResult createStoreCheckoutSession(StoreOrder order);

  StripeCheckoutResult createListingDepositCheckoutSession(ListingDeposit deposit);

  StripeWebhookEvent verifyAndParseWebhook(String payload, String signature);
}
