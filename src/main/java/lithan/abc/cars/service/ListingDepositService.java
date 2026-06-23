package lithan.abc.cars.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lithan.abc.cars.entity.ListingDeposit;
import lithan.abc.cars.payment.StripeWebhookEvent;

public interface ListingDepositService {

  boolean isStripeEnabled();

  String startCheckout(int listingId);

  Page<ListingDeposit> currentUserDeposits(Pageable pageable);

  ListingDeposit currentUserDepositBySession(String sessionId);

  boolean processWebhook(StripeWebhookEvent event);
}
