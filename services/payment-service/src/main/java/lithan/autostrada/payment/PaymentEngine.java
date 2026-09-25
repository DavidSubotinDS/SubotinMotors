package lithan.autostrada.payment;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
class PaymentEngine {
  private final PaymentStore store; private final PaymentProvider provider; private final Set<String> currencies;
  PaymentEngine(PaymentStore store,PaymentProvider provider,@Value("${payment.accepted-currencies:eur}") String configured){
    this.store=store;this.provider=provider;var values=new LinkedHashSet<String>();
    Arrays.stream(configured.split(",")).map(String::trim).map(String::toLowerCase).filter(s->!s.isBlank()).forEach(values::add);
    if(values.isEmpty())throw new IllegalArgumentException("At least one payment currency is required");currencies=Set.copyOf(values);
  }
  PaymentContracts.Capability capability(){return new PaymentContracts.Capability(provider.enabled(),currencies.stream().sorted().toList(),provider.enabled()?"sandbox":"disabled");}
  PaymentContracts.Prepared create(PaymentContracts.CreatePayment request,String idempotencyKey,Collection<String> authorities){
    validate(request,idempotencyKey,authorities);if(!provider.enabled())throw new Unavailable("Payment provider is disabled");
    PaymentContracts.Prepared prepared=store.prepare(normalize(request));
    if(!prepared.created()&&!Set.of("CREATING","RECONCILE_REQUIRED").contains(prepared.attempt().status()))return prepared;
    return new PaymentContracts.Prepared(provide(prepared.attempt()),prepared.created());
  }
  PaymentStore.Attempt findPayment(String id){return store.require(id);}
  PaymentStore.Attempt findAttempt(String id){return store.findByAttemptId(id).orElseThrow(PaymentStore.NotFound::new);}
  PaymentStore.Attempt findProviderSession(String id){return store.findBySession(id).orElseThrow(PaymentStore.NotFound::new);}
  PaymentStore.Attempt expire(String id,String idempotencyKey){
    if(idempotencyKey==null||idempotencyKey.isBlank()||idempotencyKey.length()>100)throw new Invalid("A stable Idempotency-Key is required");
    PaymentStore.Attempt a=store.require(id);if(!Set.of("SUCCEEDED","FAILED","EXPIRED").contains(a.status()))provider.expire(a);
    return store.requestExpiry(id);
  }
  void webhook(String payload,String signature){if(signature==null||signature.isBlank())throw new Invalid("Missing provider signature");store.receipt(provider.verify(payload,signature));}
  @Scheduled(fixedDelayString="${payment.reconciliation.poll-ms:5000}")
  void reconcile(){
    store.processUnmatched(20);if(!provider.enabled())return;
    for(String id:store.dueCreation(20)){try{provide(store.require(id));}catch(RuntimeException ignored){}}
    for(String id:store.dueProviderCheck(20)){
      try{provider.retrieve(store.require(id)).ifPresentOrElse(s->store.applyProviderState(id,s),()->store.deferProviderCheck(id));}
      catch(RuntimeException ignored){store.deferProviderCheck(id);}
    }
  }
  private PaymentStore.Attempt provide(PaymentStore.Attempt a){try{return store.ready(a.paymentId(),provider.create(a));}catch(PaymentProviderException failure){return store.retryLater(a.paymentId(),failure.getClass().getSimpleName());}}
  private PaymentContracts.CreatePayment normalize(PaymentContracts.CreatePayment r){return new PaymentContracts.CreatePayment(r.attemptId().trim(),r.sourceService().trim(),r.businessType().trim(),r.businessId().trim(),r.businessVersion(),r.buyerId().trim(),r.amountMinor(),r.currency().trim().toLowerCase(),r.description().trim(),r.returnRoute().trim(),r.customerEmail()==null?null:r.customerEmail().trim());}
  private void validate(PaymentContracts.CreatePayment r,String key,Collection<String> authorities){
    if(r==null||blank(r.attemptId())||blank(r.sourceService())||blank(r.businessType())||blank(r.businessId())||blank(r.buyerId())||blank(r.currency())||blank(r.description())||blank(r.returnRoute()))throw new Invalid("All payment request fields are required");
    if(!r.attemptId().equals(key))throw new Invalid("Idempotency-Key must equal the persisted attempt ID");
    try{UUID.fromString(r.attemptId());}catch(RuntimeException e){throw new Invalid("attemptId must be a UUID");}
    if(r.amountMinor()<=0||r.businessVersion()<0)throw new Invalid("Payment amount and version are invalid");
    if(!currencies.contains(r.currency().toLowerCase()))throw new Invalid("Currency is not accepted");
    boolean store="commerce-service".equals(r.sourceService())&&"STORE_ORDER".equals(r.businessType())&&"STORE_ORDER".equals(r.returnRoute())&&authorities.contains("SCOPE_create-store-payment");
    boolean deposit="marketplace-service".equals(r.sourceService())&&"LISTING_DEPOSIT".equals(r.businessType())&&"LISTING_DEPOSIT".equals(r.returnRoute())&&authorities.contains("SCOPE_create-deposit-payment");
    if(!store&&!deposit)throw new Forbidden("Caller is not granted this payment purpose");
    if(r.description().length()>255||r.businessId().length()>80||r.buyerId().length()>80||(r.customerEmail()!=null&&r.customerEmail().length()>254))throw new Invalid("Payment request field is too long");
  }
  private static boolean blank(String value){return value==null||value.isBlank();}
  static final class Invalid extends RuntimeException{Invalid(String m){super(m);}}
  static final class Forbidden extends RuntimeException{Forbidden(String m){super(m);}}
  static final class Unavailable extends RuntimeException{Unavailable(String m){super(m);}}
}
