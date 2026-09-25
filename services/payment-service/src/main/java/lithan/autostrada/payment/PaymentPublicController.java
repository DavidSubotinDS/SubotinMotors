package lithan.autostrada.payment;

import org.springframework.http.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
class PaymentPublicController {
  private final PaymentEngine engine;
  PaymentPublicController(PaymentEngine engine){this.engine=engine;}
  @GetMapping("/{id}") ResponseEntity<PaymentContracts.PaymentView> payment(@PathVariable String id,JwtAuthenticationToken auth){
    PaymentStore.Attempt a=engine.findPayment(id);
    boolean admin=auth.getAuthorities().stream().anyMatch(v->"ROLE_ADMIN".equals(v.getAuthority()));
    if(!admin&&!a.buyerId().equals(auth.getName()))throw new PaymentEngine.Forbidden("You cannot view another account's payment");
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new PaymentContracts.PaymentView(a.paymentId(),a.attemptId(),a.sourceService(),a.businessType(),a.businessId(),a.businessVersion(),a.buyerId(),a.amountMinor(),a.currency(),a.status(),a.checkoutUrl(),a.expiresAt(),a.aggregateVersion()));
  }
}
