package lithan.abc.cars.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_payment_webhook_event")
public class PaymentWebhookEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_webhook_event")
  private int idWebhookEvent;

  @Column(name = "provider_event_id", nullable = false, unique = true)
  private String providerEventId;

  @Column(name = "event_type", nullable = false)
  private String eventType;

  @Column(name = "processed_at", nullable = false)
  private Instant processedAt;

  public void setProviderEventId(String providerEventId) {
    this.providerEventId = providerEventId;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public void setProcessedAt(Instant processedAt) {
    this.processedAt = processedAt;
  }
}
