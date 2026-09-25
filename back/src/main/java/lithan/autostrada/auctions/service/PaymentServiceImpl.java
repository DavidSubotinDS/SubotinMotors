package lithan.autostrada.auctions.service;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lithan.autostrada.auctions.entity.*;
import lithan.autostrada.auctions.identity.CurrentIdentity;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;
import lithan.autostrada.auctions.repository.*;

/** Historical auction-payment audit reader retained until marketplace extraction. */
@Service
public class PaymentServiceImpl implements PaymentService {
  private final CurrentIdentity identity;private final PaymentAccountRepository accounts;
  private final PaymentOrderRepository payments;private final PaymentWebhookEventRepository webhooks;
  public PaymentServiceImpl(CurrentIdentity identity,PaymentAccountRepository accounts,PaymentOrderRepository payments,PaymentWebhookEventRepository webhooks){
    this.identity=identity;this.accounts=accounts;this.payments=payments;this.webhooks=webhooks;
  }
  public boolean isStripeEnabled(){return false;}
  public Optional<PaymentAccount> getCurrentSellerAccount(){return accounts.findByUserId(identity.requireUserId());}
  public String startSellerOnboarding(){throw retired();}
  public PaymentAccount refreshCurrentSellerAccount(){throw retired();}
  public PaymentOrder acceptBidForPayment(int bidId){throw retired();}
  public String createBuyerCheckout(int paymentId){throw retired();}
  public Page<PaymentOrder> listCurrentUserPurchases(Pageable pageable){return payments.findByBuyerId(identity.requireUserId(),pageable);}
  public Page<PaymentOrder> listCurrentUserSales(Pageable pageable){return payments.findBySellerId(identity.requireUserId(),pageable);}
  public Page<PaymentOrder> listAllPayments(Pageable pageable){return payments.findAll(pageable);}
  public Page<PaymentWebhookEvent> listWebhookEvents(Pageable pageable){return webhooks.findAll(pageable);}
  public Optional<PaymentOrder> findCurrentBuyerPaymentBySession(String id){return payments.findByCheckoutSessionId(id).filter(p->p.getBuyerId()==identity.requireUserId());}
  public void processWebhook(StripeWebhookEvent event){throw retired();}
  private static IllegalStateException retired(){return new IllegalStateException("Legacy auction payment checkout is retired");}
}
