package lithan.abc.cars.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lithan.abc.cars.entity.StoreOrder;
import lithan.abc.cars.payment.StripeWebhookEvent;

public interface StoreOrderService {
  boolean isStripeEnabled();

  String startCheckout();

  Page<StoreOrder> currentUserOrders(Pageable pageable);

  Page<StoreOrder> allOrders(Pageable pageable);

  StoreOrder currentUserOrder(int idOrder);

  StoreOrder currentUserOrderBySession(String sessionId);

  boolean processWebhook(StripeWebhookEvent event);
}
