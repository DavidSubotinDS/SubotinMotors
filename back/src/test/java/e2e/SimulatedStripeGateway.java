package e2e;

import lithan.autostrada.auctions.config.StripeProperties;
import lithan.autostrada.auctions.entity.*;
import lithan.autostrada.auctions.payment.*;

/** Simulates outbound checkout creation only. Inbound verification uses production Stripe code. */
final class SimulatedStripeGateway implements StripeGateway {
  private final StripeConnectGateway verifier;

  SimulatedStripeGateway() {
    StripeProperties properties = new StripeProperties();
    properties.setEnabled(true);
    properties.setSecretKey("sk_test_e2e_placeholder_not_a_credential");
    properties.setWebhookSecret("whsec_e2e_public_fixture_secret");
    verifier = new StripeConnectGateway(properties);
  }

  @Override public boolean isEnabled() { return true; }
  @Override public StripeCheckoutResult createStoreCheckoutSession(StoreOrder order) {
    return checkout("store", order.getIdOrder());
  }
  @Override public StripeCheckoutResult createListingDepositCheckoutSession(ListingDeposit deposit) {
    return checkout("deposit", deposit.getIdDeposit());
  }
  private StripeCheckoutResult checkout(String purpose, int id) {
    String session = "cs_e2e_" + purpose + "_" + id;
    return new StripeCheckoutResult(session, "http://127.0.0.1:15173/__provider/checkout?session_id=" + session);
  }
  @Override public StripeWebhookEvent verifyAndParseWebhook(String payload, String signature) {
    return verifier.verifyAndParseWebhook(payload, signature);
  }
  @Override public String createConnectedAccount(UserAccount seller) { throw retired(); }
  @Override public String createOnboardingLink(String account) { throw retired(); }
  @Override public StripeAccountState retrieveAccountState(String account) { throw retired(); }
  @Override public StripeCheckoutResult createCheckoutSession(PaymentOrder order, String account) { throw retired(); }
  private UnsupportedOperationException retired() { return new UnsupportedOperationException("Retired auction payments"); }
}
