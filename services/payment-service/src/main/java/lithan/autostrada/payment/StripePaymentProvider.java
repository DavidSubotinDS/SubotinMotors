package lithan.autostrada.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.stripe.StripeClient;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;

@Component
@ConditionalOnProperty(name="payment.provider.enabled",havingValue="true")
class StripePaymentProvider implements PaymentProvider {
  private final StripeClient client; private final String webhookSecret; private final String publicUrl;
  StripePaymentProvider(@Value("${payment.provider.secret-key}")String key,
      @Value("${payment.provider.webhook-secret}")String secret,@Value("${payment.public-url}")String url){
    if(key.isBlank()||secret.isBlank())throw new IllegalArgumentException("Stripe secrets are required only when provider mode is enabled");
    client=new StripeClient(key);webhookSecret=secret;publicUrl=url.replaceAll("/+$","");
  }
  public boolean enabled(){return true;}
  public PaymentContracts.ProviderResult create(PaymentStore.Attempt a){
    try{
      var product=SessionCreateParams.LineItem.PriceData.ProductData.builder().setName(a.description()).build();
      var price=SessionCreateParams.LineItem.PriceData.builder().setCurrency(a.currency()).setUnitAmount(a.amountMinor()).setProductData(product).build();
      String success="STORE_ORDER".equals(a.businessType())?"/store/checkout/success":"/listing-deposits/success";
      String cancel="STORE_ORDER".equals(a.businessType())?"/cart?checkoutCanceled":"/user/listing-deposits?depositCanceled";
      var builder=SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT)
          .setClientReferenceId(a.paymentId()).setSuccessUrl(publicUrl+success+"?session_id={CHECKOUT_SESSION_ID}")
          .setCancelUrl(publicUrl+cancel).addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
          .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L).setPriceData(price).build())
          .putMetadata("payment_id",a.paymentId()).putMetadata("attempt_id",a.attemptId())
          .putMetadata("source_service",a.sourceService()).putMetadata("business_type",a.businessType())
          .putMetadata("business_id",a.businessId());
      if(a.customerEmail()!=null&&!a.customerEmail().isBlank())builder.setCustomerEmail(a.customerEmail());
      Session session=client.checkout().sessions().create(builder.build(),RequestOptions.builder().setIdempotencyKey(a.attemptId()).build());
      return new PaymentContracts.ProviderResult(session.getId(),session.getUrl(),session.getPaymentIntent());
    }catch(Exception e){throw new PaymentProviderException("Stripe checkout creation failed",e);}
  }
  public PaymentContracts.ProviderEvent verify(String payload,String signature){
    try{
      Event event=client.constructEvent(payload,signature,webhookSecret);StripeObject value=event.getDataObjectDeserializer().getObject().orElse(null);
      if(value instanceof Session s)return new PaymentContracts.ProviderEvent(event.getId(),event.getType(),s.getId(),s.getPaymentIntent(),s.getPaymentStatus(),hash(payload));
      return new PaymentContracts.ProviderEvent(event.getId(),event.getType(),null,null,null,hash(payload));
    }catch(Exception e){throw new PaymentProviderException("Invalid Stripe webhook",e);}
  }
  public void expire(PaymentStore.Attempt a){
    if(a.providerSessionId()==null)return;
    try{client.checkout().sessions().expire(a.providerSessionId());}catch(Exception e){throw new PaymentProviderException("Stripe expiry failed",e);}
  }
  public java.util.Optional<PaymentContracts.ProviderState> retrieve(PaymentStore.Attempt a){
    if(a.providerSessionId()==null)return java.util.Optional.empty();
    try{
      Session s=client.checkout().sessions().retrieve(a.providerSessionId());
      return java.util.Optional.of(new PaymentContracts.ProviderState(s.getId(),s.getPaymentIntent(),s.getPaymentStatus(),s.getStatus()));
    }catch(Exception e){throw new PaymentProviderException("Stripe reconciliation failed",e);}
  }
  private static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
