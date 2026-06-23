package lithan.autostrada.auctions.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lithan.autostrada.auctions.entity.PaymentAccount;
import lithan.autostrada.auctions.entity.PaymentOrder;
import lithan.autostrada.auctions.entity.PaymentWebhookEvent;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;

public interface PaymentService {
  boolean isStripeEnabled();

  Optional<PaymentAccount> getCurrentSellerAccount();

  String startSellerOnboarding();

  PaymentAccount refreshCurrentSellerAccount();

  PaymentOrder acceptBidForPayment(int bidId);

  String createBuyerCheckout(int paymentId);

  Page<PaymentOrder> listCurrentUserPurchases(Pageable pageable);

  Page<PaymentOrder> listCurrentUserSales(Pageable pageable);

  Page<PaymentOrder> listAllPayments(Pageable pageable);

  Page<PaymentWebhookEvent> listWebhookEvents(Pageable pageable);

  Optional<PaymentOrder> findCurrentBuyerPaymentBySession(String sessionId);

  void processWebhook(StripeWebhookEvent event);
}
