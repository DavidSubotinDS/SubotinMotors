package lithan.autostrada.payment;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
class PaymentWebhookController {
  private final PaymentEngine engine;
  PaymentWebhookController(PaymentEngine engine){this.engine=engine;}
  @PostMapping(path="/webhooks/stripe",consumes=MediaType.ALL_VALUE)
  ResponseEntity<Void> stripe(@RequestBody String raw,@RequestHeader(name="Stripe-Signature",required=false)String signature){
    engine.webhook(raw,signature);return ResponseEntity.ok().build();
  }
}
