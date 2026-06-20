package lithan.abc.cars.service;

import java.util.List;
import java.util.Optional;

import lithan.abc.cars.entity.PaymentAccount;
import lithan.abc.cars.entity.PaymentOrder;
import lithan.abc.cars.payment.StripeWebhookEvent;

public interface PaymentService {
  boolean isStripeEnabled();

  Optional<PaymentAccount> getCurrentSellerAccount();

  String startSellerOnboarding();

  PaymentAccount refreshCurrentSellerAccount();

  PaymentOrder acceptBidForPayment(int bidId);

  String createBuyerCheckout(int paymentId);

  List<PaymentOrder> listCurrentUserPurchases();

  List<PaymentOrder> listCurrentUserSales();

  Optional<PaymentOrder> findCurrentBuyerPaymentBySession(String sessionId);

  void processWebhook(StripeWebhookEvent event);
}
