package lithan.autostrada.auctions.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import lithan.autostrada.auctions.entity.CheckoutWebhookInbox;

public interface CheckoutWebhookInboxRepository extends JpaRepository<CheckoutWebhookInbox, Long> {
  Optional<CheckoutWebhookInbox> findByProviderEventId(String providerEventId);
  List<CheckoutWebhookInbox> findByStatusOrderByReceivedAtAsc(String status, Pageable pageable);
}
