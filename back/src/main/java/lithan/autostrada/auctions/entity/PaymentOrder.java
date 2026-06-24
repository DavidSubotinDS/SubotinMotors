package lithan.autostrada.auctions.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "tb_payment_order")
public class PaymentOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_payment")
  private int idPayment;

  @OneToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_bid", nullable = false, unique = true)
  private CarBidding bid;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_buyer", nullable = false)
  private UserAccount buyer;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_seller", nullable = false)
  private UserAccount seller;

  @Column(name = "amount_minor", nullable = false)
  private long amountMinor;

  @Column(name = "platform_fee_minor", nullable = false)
  private long platformFeeMinor;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, length = 30)
  private String status;

  @Column(nullable = false, length = 30)
  private String purpose = "AUCTION_PURCHASE";

  @Column(name = "checkout_session_id", unique = true)
  private String checkoutSessionId;

  @Column(name = "checkout_url", length = 2000)
  private String checkoutUrl;

  @Column(name = "payment_intent_id", unique = true)
  private String paymentIntentId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Version
  private long version;

  public int getIdPayment() {
    return idPayment;
  }

  public CarBidding getBid() {
    return bid;
  }

  public void setBid(CarBidding bid) {
    this.bid = bid;
  }

  public UserAccount getBuyer() {
    return buyer;
  }

  public void setBuyer(UserAccount buyer) {
    this.buyer = buyer;
  }

  public UserAccount getSeller() {
    return seller;
  }

  public void setSeller(UserAccount seller) {
    this.seller = seller;
  }

  public long getAmountMinor() {
    return amountMinor;
  }

  public void setAmountMinor(long amountMinor) {
    this.amountMinor = amountMinor;
  }

  public long getPlatformFeeMinor() {
    return platformFeeMinor;
  }

  public void setPlatformFeeMinor(long platformFeeMinor) {
    this.platformFeeMinor = platformFeeMinor;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPurpose() {
    return purpose;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  public String getCheckoutSessionId() {
    return checkoutSessionId;
  }

  public void setCheckoutSessionId(String checkoutSessionId) {
    this.checkoutSessionId = checkoutSessionId;
  }

  public String getCheckoutUrl() {
    return checkoutUrl;
  }

  public void setCheckoutUrl(String checkoutUrl) {
    this.checkoutUrl = checkoutUrl;
  }

  public String getPaymentIntentId() {
    return paymentIntentId;
  }

  public void setPaymentIntentId(String paymentIntentId) {
    this.paymentIntentId = paymentIntentId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(Instant paidAt) {
    this.paidAt = paidAt;
  }
}
