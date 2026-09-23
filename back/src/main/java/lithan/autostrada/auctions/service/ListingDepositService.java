package lithan.autostrada.auctions.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lithan.autostrada.auctions.entity.ListingDeposit;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;

public interface ListingDepositService {

  boolean isStripeEnabled();

  CheckoutOutcome startCheckout(int listingId, String requestId);

  default String startCheckout(int listingId) {
    return startCheckout(listingId, null).checkoutUrl();
  }

  Page<ListingDeposit> currentUserDeposits(Pageable pageable);

  ListingDeposit currentUserDepositBySession(String sessionId);

  boolean processWebhook(StripeWebhookEvent event);
}
