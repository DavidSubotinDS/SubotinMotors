package lithan.autostrada.auctions.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "tb_checkout_attempt")
public class CheckoutAttempt {
  @Id
  @Column(name = "attempt_id", length = 36)
  private String attemptId;
  @Column(name = "id_user", nullable = false)
  private int userId;
  @Column(nullable = false, length = 30)
  private String purpose;
  @Column(name = "client_request_id", nullable = false, length = 100)
  private String clientRequestId;
  @Column(name = "request_hash", nullable = false, length = 64, columnDefinition = "char(64)")
  private String requestHash;
  @Column(name = "customer_email", nullable = false, length = 254)
  private String customerEmail;
  @Column(name = "aggregate_id")
  private Integer aggregateId;
  @Column(nullable = false, length = 30)
  private String status;
  @Column(name = "provider_session_id", unique = true)
  private String providerSessionId;
  @Column(name = "provider_checkout_url", length = 2000)
  private String providerCheckoutUrl;
  @Column(name = "payment_intent_id")
  private String paymentIntentId;
  @Column(name = "failure_count", nullable = false)
  private int failureCount;
  @Column(name = "last_failure_code", length = 80)
  private String lastFailureCode;
  @Column(name = "next_reconcile_at")
  private Instant nextReconcileAt;
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
  @Version
  private long version;

  public String getAttemptId() { return attemptId; }
  public void setAttemptId(String value) { attemptId = value; }
  public int getUserId() { return userId; }
  public void setUserId(int value) { userId = value; }
  public String getPurpose() { return purpose; }
  public void setPurpose(String value) { purpose = value; }
  public String getClientRequestId() { return clientRequestId; }
  public void setClientRequestId(String value) { clientRequestId = value; }
  public String getRequestHash() { return requestHash; }
  public void setRequestHash(String value) { requestHash = value; }
  public String getCustomerEmail() { return customerEmail; }
  public void setCustomerEmail(String value) { customerEmail = value; }
  public Integer getAggregateId() { return aggregateId; }
  public void setAggregateId(Integer value) { aggregateId = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public String getProviderSessionId() { return providerSessionId; }
  public void setProviderSessionId(String value) { providerSessionId = value; }
  public String getProviderCheckoutUrl() { return providerCheckoutUrl; }
  public void setProviderCheckoutUrl(String value) { providerCheckoutUrl = value; }
  public String getPaymentIntentId() { return paymentIntentId; }
  public void setPaymentIntentId(String value) { paymentIntentId = value; }
  public int getFailureCount() { return failureCount; }
  public void setFailureCount(int value) { failureCount = value; }
  public String getLastFailureCode() { return lastFailureCode; }
  public void setLastFailureCode(String value) { lastFailureCode = value; }
  public Instant getNextReconcileAt() { return nextReconcileAt; }
  public void setNextReconcileAt(Instant value) { nextReconcileAt = value; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant value) { expiresAt = value; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant value) { createdAt = value; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant value) { updatedAt = value; }
}
