package lithan.autostrada.auctions.payment;

import lithan.autostrada.auctions.entity.PaymentOrder;
import lithan.autostrada.auctions.entity.ListingDeposit;
import lithan.autostrada.auctions.entity.StoreOrder;

public interface StripeGateway {
  boolean isEnabled();

  String createConnectedAccount(int sellerId, String displayName);

  String createOnboardingLink(String accountId);

  StripeAccountState retrieveAccountState(String accountId);

  StripeCheckoutResult createCheckoutSession(PaymentOrder paymentOrder, String destinationAccountId);

  StripeCheckoutResult createStoreCheckoutSession(
      StoreOrder order, String customerEmail, String idempotencyKey);

  StripeCheckoutResult createListingDepositCheckoutSession(
      ListingDeposit deposit, String customerEmail, String idempotencyKey);

  String findAttemptByProviderSession(String sessionId);

  StripeWebhookEvent verifyAndParseWebhook(String payload, String signature);
}
