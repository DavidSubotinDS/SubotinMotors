package lithan.autostrada.auctions.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import lithan.autostrada.auctions.entity.PaymentWebhookEvent;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Integer> {
  boolean existsByProviderEventId(String providerEventId);
}
