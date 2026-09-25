package lithan.autostrada.payment;

import java.net.URI;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/v1")
class PaymentController {
  private final PaymentEngine engine;
  PaymentController(PaymentEngine engine){this.engine=engine;}
  @GetMapping("/capabilities") PaymentContracts.Capability capabilities(){return engine.capability();}
  @PostMapping("/payments") ResponseEntity<PaymentContracts.PaymentView> create(@RequestBody PaymentContracts.CreatePayment request,
      @RequestHeader(name="Idempotency-Key",required=false)String key,Authentication authentication){
    var authorities=authentication.getAuthorities().stream().map(a->a.getAuthority()).toList();
    var result=engine.create(request,key,authorities);var view=view(result.attempt());
    if(Set.of("CREATING","RECONCILE_REQUIRED").contains(result.attempt().status()))return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(view);
    if(result.created())return ResponseEntity.created(URI.create("/internal/v1/payments/"+result.attempt().paymentId())).cacheControl(CacheControl.noStore()).body(view);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view);
  }
  @GetMapping("/payments/{id}") ResponseEntity<PaymentContracts.PaymentView> byPayment(@PathVariable String id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view(engine.findPayment(id)));}
  @GetMapping("/payment-attempts/{id}") ResponseEntity<PaymentContracts.PaymentView> byAttempt(@PathVariable String id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view(engine.findAttempt(id)));}
  @GetMapping("/provider-sessions/{id}") ResponseEntity<PaymentContracts.PaymentView> byProviderSession(@PathVariable String id){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(view(engine.findProviderSession(id)));}
  @PostMapping("/payments/{id}/expire") ResponseEntity<PaymentContracts.PaymentView> expire(@PathVariable String id,@RequestHeader(name="Idempotency-Key",required=false)String key){var result=engine.expire(id,key);return ResponseEntity.status("EXPIRY_REQUESTED".equals(result.status())?HttpStatus.ACCEPTED:HttpStatus.OK).cacheControl(CacheControl.noStore()).body(view(result));}
  private static PaymentContracts.PaymentView view(PaymentStore.Attempt a){return new PaymentContracts.PaymentView(a.paymentId(),a.attemptId(),a.sourceService(),a.businessType(),a.businessId(),a.businessVersion(),a.buyerId(),a.amountMinor(),a.currency(),a.status(),a.checkoutUrl(),a.expiresAt(),a.aggregateVersion());}
}
