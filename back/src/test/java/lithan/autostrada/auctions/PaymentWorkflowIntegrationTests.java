package lithan.autostrada.auctions;

import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lithan.autostrada.auctions.payment.StripeWebhookEvent;
import lithan.autostrada.auctions.service.PaymentService;

@org.springframework.context.annotation.Import(BusinessIdentityFixtures.class)
@SpringBootTest
@Transactional
class PaymentWorkflowIntegrationTests {
  @Autowired PaymentService payments;
  @Test void legacyAuctionProviderMutationsAreRetired(){
    assertThatThrownBy(()->payments.acceptBidForPayment(1)).isInstanceOf(IllegalStateException.class).hasMessageContaining("retired");
    assertThatThrownBy(()->payments.processWebhook(new StripeWebhookEvent("evt","checkout.session.completed","cs","pi","paid")))
        .isInstanceOf(IllegalStateException.class).hasMessageContaining("retired");
    assertThat(payments.isStripeEnabled()).isFalse();
  }
  @Test void legacyPaymentAuditDtosRemainReadable(){
    assertThat(payments.listAllPayments(Pageable.ofSize(20)).getContent()).isNotEmpty();
    assertThat(payments.listWebhookEvents(Pageable.ofSize(20)).getContent()).isNotEmpty();
  }
}
