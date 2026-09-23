package lithan.autostrada.auctions.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lithan.autostrada.auctions.entity.StoreOrder;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;

public interface StoreOrderService {
  boolean isStripeEnabled();

  CheckoutOutcome startCheckout(String requestId);

  default String startCheckout() {
    return startCheckout(null).checkoutUrl();
  }

  Page<StoreOrder> currentUserOrders(Pageable pageable);

  Page<StoreOrder> allOrders(Pageable pageable);

  StoreOrder adminOrder(int idOrder);

  StoreOrder currentUserOrder(int idOrder);

  StoreOrder currentUserOrderBySession(String sessionId);

  boolean processWebhook(StripeWebhookEvent event);
}
