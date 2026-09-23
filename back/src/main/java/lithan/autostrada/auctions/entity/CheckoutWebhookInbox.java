package lithan.autostrada.auctions.entity;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "tb_checkout_webhook_inbox")
public class CheckoutWebhookInbox {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_inbox")
  private long idInbox;
  @Column(name = "provider_event_id", nullable = false, unique = true)
  private String providerEventId;
  @Column(name = "event_type", nullable = false)
  private String eventType;
  @Column(name = "checkout_session_id")
  private String checkoutSessionId;
  @Column(name = "payment_intent_id")
  private String paymentIntentId;
  @Column(name = "payment_status", length = 40)
  private String paymentStatus;
  @Column(nullable = false, length = 20)
  private String status;
  @Column(name = "delivery_count", nullable = false)
  private int deliveryCount;
  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
  @Column(name = "processed_at")
  private Instant processedAt;

  public long getIdInbox() { return idInbox; }
  public String getProviderEventId() { return providerEventId; }
  public void setProviderEventId(String value) { providerEventId = value; }
  public String getEventType() { return eventType; }
  public void setEventType(String value) { eventType = value; }
  public String getCheckoutSessionId() { return checkoutSessionId; }
  public void setCheckoutSessionId(String value) { checkoutSessionId = value; }
  public String getPaymentIntentId() { return paymentIntentId; }
  public void setPaymentIntentId(String value) { paymentIntentId = value; }
  public String getPaymentStatus() { return paymentStatus; }
  public void setPaymentStatus(String value) { paymentStatus = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public int getDeliveryCount() { return deliveryCount; }
  public void setDeliveryCount(int value) { deliveryCount = value; }
  public Instant getReceivedAt() { return receivedAt; }
  public void setReceivedAt(Instant value) { receivedAt = value; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant value) { updatedAt = value; }
  public Instant getProcessedAt() { return processedAt; }
  public void setProcessedAt(Instant value) { processedAt = value; }
}

