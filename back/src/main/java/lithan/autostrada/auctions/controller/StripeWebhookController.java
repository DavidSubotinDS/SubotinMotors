package lithan.autostrada.auctions.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import lithan.autostrada.auctions.payment.StripeGateway;
import lithan.autostrada.auctions.payment.PaymentProviderException;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;
import lithan.autostrada.auctions.service.CheckoutWebhookInboxService;

@RestController
public class StripeWebhookController {

  private final StripeGateway stripeGateway;
  private final CheckoutWebhookInboxService webhookInbox;

  public StripeWebhookController(
      StripeGateway stripeGateway,
      CheckoutWebhookInboxService webhookInbox) {
    this.stripeGateway = stripeGateway;
    this.webhookInbox = webhookInbox;
  }

  @PostMapping("/webhooks/stripe")
  public ResponseEntity<Void> handle(
      @RequestBody String payload,
      @RequestHeader("Stripe-Signature") String signature) {
    try {
      StripeWebhookEvent event = stripeGateway.verifyAndParseWebhook(payload, signature);
      webhookInbox.receive(event);
      return ResponseEntity.ok().build();
    } catch (PaymentProviderException exception) {
      return ResponseEntity.badRequest().build();
    }
  }
}
