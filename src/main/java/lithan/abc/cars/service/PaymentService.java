package lithan.abc.cars.service;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

  Page<PaymentOrder> listCurrentUserPurchases(Pageable pageable);

  Page<PaymentOrder> listCurrentUserSales(Pageable pageable);

  Page<PaymentOrder> listAllPayments(Pageable pageable);

  Optional<PaymentOrder> findCurrentBuyerPaymentBySession(String sessionId);

  void processWebhook(StripeWebhookEvent event);
}
