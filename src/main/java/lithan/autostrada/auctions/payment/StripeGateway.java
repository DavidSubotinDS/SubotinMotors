package lithan.autostrada.auctions.payment;

import lithan.autostrada.auctions.entity.PaymentOrder;
import lithan.autostrada.auctions.entity.ListingDeposit;
import lithan.autostrada.auctions.entity.StoreOrder;
import lithan.autostrada.auctions.entity.UserAccount;

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
