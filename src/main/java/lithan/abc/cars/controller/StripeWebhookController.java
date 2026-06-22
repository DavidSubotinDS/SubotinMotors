package lithan.abc.cars.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import lithan.abc.cars.payment.StripeGateway;
import lithan.abc.cars.payment.PaymentProviderException;
import lithan.abc.cars.payment.StripeWebhookEvent;
import lithan.abc.cars.service.PaymentService;
import lithan.abc.cars.service.StoreOrderService;

@RestController
public class StripeWebhookController {

  private final StripeGateway stripeGateway;
  private final PaymentService paymentService;
  private final StoreOrderService storeOrderService;

  public StripeWebhookController(
      StripeGateway stripeGateway,
      PaymentService paymentService,
      StoreOrderService storeOrderService) {
    this.stripeGateway = stripeGateway;
    this.paymentService = paymentService;
    this.storeOrderService = storeOrderService;
  }

  @PostMapping("/webhooks/stripe")
  public ResponseEntity<Void> handle(
      @RequestBody String payload,
      @RequestHeader("Stripe-Signature") String signature) {
    try {
      StripeWebhookEvent event = stripeGateway.verifyAndParseWebhook(payload, signature);
      if (!storeOrderService.processWebhook(event)) {
        paymentService.processWebhook(event);
      }
      return ResponseEntity.ok().build();
    } catch (PaymentProviderException exception) {
      return ResponseEntity.badRequest().build();
    }
  }
}
