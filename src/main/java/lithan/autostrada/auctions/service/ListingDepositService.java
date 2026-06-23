package lithan.autostrada.auctions.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lithan.autostrada.auctions.entity.ListingDeposit;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;

public interface ListingDepositService {

  boolean isStripeEnabled();

  String startCheckout(int listingId);

  Page<ListingDeposit> currentUserDeposits(Pageable pageable);

  ListingDeposit currentUserDepositBySession(String sessionId);

  boolean processWebhook(StripeWebhookEvent event);
}
