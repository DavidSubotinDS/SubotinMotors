package lithan.autostrada.auctions.payment;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import lithan.autostrada.auctions.entity.*;

/** Bounded, no-retry payment client. The attempt UUID is the remote idempotency key. */
@Component
public class RemotePaymentGateway implements StripeGateway {
  private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(RemotePaymentGateway.class);
  private final RestClient identity,payment;private final String secret;private final Semaphore capacity=new Semaphore(24);
  private final java.util.concurrent.atomic.AtomicBoolean dependencyUnavailable=new java.util.concurrent.atomic.AtomicBoolean();
  public RemotePaymentGateway(@Value("${identity.base-url}")String identityUrl,@Value("${identity.service-secret}")String secret,
      @Value("${payment.base-url:http://127.0.0.1:8084}")String paymentUrl){
    var transport=new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofMillis(300)).followRedirects(java.net.http.HttpClient.Redirect.NEVER).build());
    transport.setReadTimeout(Duration.ofSeconds(2));identity=RestClient.builder().baseUrl(identityUrl).requestFactory(transport).build();
    payment=RestClient.builder().baseUrl(paymentUrl).requestFactory(transport).build();this.secret=secret;
  }
  private record Token(String accessToken){@Override public String toString(){return "Token[redacted]";}}
  private record Capability(boolean enabled,List<String> acceptedCurrencies,String mode){}
  private record Request(String attemptId,String sourceService,String businessType,String businessId,long businessVersion,String buyerId,long amountMinor,String currency,String description,String returnRoute,String customerEmail){}
  private record Result(String paymentId,String attemptId,String status,String checkoutUrl){}
  private String token(){return identity.post().uri("/internal/v1/service-token").headers(h->h.setBasicAuth("legacy-backend",secret)).body(Map.of("audience","payment-service")).retrieve().body(Token.class).accessToken();}
  public boolean isEnabled(){
    String assertion;
    try{assertion=token();}catch(RuntimeException unavailable){logFailure("identity-token",unavailable);return false;}
    try{boolean enabled=invoke(()->payment.get().uri("/internal/v1/capabilities").headers(h->h.setBearerAuth(assertion)).retrieve().body(Capability.class)).enabled();
      if(dependencyUnavailable.getAndSet(false))log.info("Payment dependency recovered");return enabled;}
    catch(RuntimeException unavailable){logFailure("payment-capability",unavailable);return false;}
  }
  private void logFailure(String stage,RuntimeException failure){
    if(!dependencyUnavailable.compareAndSet(false,true))return;
    String status=failure instanceof org.springframework.web.client.RestClientResponseException response
        ?Integer.toString(response.getStatusCode().value()):"none";
    Throwable root=failure;while(root.getCause()!=null)root=root.getCause();
    log.warn("Payment dependency request failed: stage={} exception_type={} root_type={} status={}",stage,
        failure.getClass().getName(),root.getClass().getName(),status);
  }
  public StripeCheckoutResult createStoreCheckoutSession(StoreOrder order,String email,String key){return create(new Request(key,"commerce-service","STORE_ORDER",Integer.toString(order.getIdOrder()),1,Integer.toString(order.getUserId()),order.getTotalMinor(),order.getCurrency(),"Order "+order.getIdOrder(),"STORE_ORDER",email));}
  public StripeCheckoutResult createListingDepositCheckoutSession(ListingDeposit deposit,String email,String key){return create(new Request(key,"marketplace-service","LISTING_DEPOSIT",Integer.toString(deposit.getIdDeposit()),1,Integer.toString(deposit.getBuyerId()),deposit.getAmountMinor(),deposit.getCurrency(),"Listing deposit "+deposit.getIdDeposit(),"LISTING_DEPOSIT",email));}
  private StripeCheckoutResult create(Request request){try{Result result=invoke(()->payment.post().uri("/internal/v1/payments").headers(h->{h.setBearerAuth(token());h.set("Idempotency-Key",request.attemptId());}).body(request).retrieve().body(Result.class));return new StripeCheckoutResult(result==null?null:result.paymentId(),result==null?null:result.checkoutUrl());}catch(RuntimeException failure){throw new PaymentProviderException("Payment service checkout is unresolved",failure);}}
  public String findAttemptByProviderSession(String sessionId){try{return invoke(()->payment.get().uri("/internal/v1/provider-sessions/{id}",sessionId).headers(h->h.setBearerAuth(token())).retrieve().body(Result.class)).attemptId();}catch(RuntimeException failure){throw new PaymentProviderException("Payment lookup is unavailable",failure);}}
  private <T>T invoke(java.util.concurrent.Callable<T> call){if(!capacity.tryAcquire())throw new PaymentProviderException("Payment client capacity exhausted");try{return call.call();}catch(Exception failure){throw failure instanceof RuntimeException runtime?runtime:new PaymentProviderException("Payment service unavailable",failure);}finally{capacity.release();}}
  public String createConnectedAccount(int sellerId,String displayName){throw retired();}
  public String createOnboardingLink(String accountId){throw retired();}
  public StripeAccountState retrieveAccountState(String accountId){throw retired();}
  public StripeCheckoutResult createCheckoutSession(PaymentOrder paymentOrder,String destinationAccountId){throw retired();}
  public StripeWebhookEvent verifyAndParseWebhook(String payload,String signature){throw retired();}
  private static UnsupportedOperationException retired(){return new UnsupportedOperationException("Legacy auction payment onboarding and checkout are retired");}
}
