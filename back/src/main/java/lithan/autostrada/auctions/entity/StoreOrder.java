package lithan.autostrada.auctions.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "tb_store_order")
public class StoreOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_order")
  private int idOrder;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_user", nullable = false)
  private UserAccount user;

  @Column(name = "total_minor", nullable = false)
  private long totalMinor;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(nullable = false, length = 30)
  private String status;

  @Column(name = "shipping_name", nullable = false, length = 120)
  private String shippingName;

  @Column(name = "shipping_address", nullable = false, length = 500)
  private String shippingAddress;

  @Column(name = "shipping_street_address", length = 255)
  private String shippingStreetAddress;

  @Column(name = "shipping_city", length = 120)
  private String shippingCity;

  @Column(name = "shipping_postal_code", length = 30)
  private String shippingPostalCode;

  @Column(name = "shipping_country", length = 120)
  private String shippingCountry;

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

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  private List<StoreOrderItem> items = new ArrayList<>();

  @Version
  private long version;

  public int getIdOrder() {
    return idOrder;
  }

  public UserAccount getUser() {
    return user;
  }

  public void setUser(UserAccount user) {
    this.user = user;
  }

  public long getTotalMinor() {
    return totalMinor;
  }

  public void setTotalMinor(long totalMinor) {
    this.totalMinor = totalMinor;
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

  public String getShippingName() {
    return shippingName;
  }

  public void setShippingName(String shippingName) {
    this.shippingName = shippingName;
  }

  public String getShippingAddress() {
    return shippingAddress;
  }

  public void setShippingAddress(String shippingAddress) {
    this.shippingAddress = shippingAddress;
  }

  public String getShippingStreetAddress() {
    return shippingStreetAddress;
  }

  public void setShippingStreetAddress(String shippingStreetAddress) {
    this.shippingStreetAddress = shippingStreetAddress;
  }

  public String getShippingCity() {
    return shippingCity;
  }

  public void setShippingCity(String shippingCity) {
    this.shippingCity = shippingCity;
  }

  public String getShippingPostalCode() {
    return shippingPostalCode;
  }

  public void setShippingPostalCode(String shippingPostalCode) {
    this.shippingPostalCode = shippingPostalCode;
  }

  public String getShippingCountry() {
    return shippingCountry;
  }

  public void setShippingCountry(String shippingCountry) {
    this.shippingCountry = shippingCountry;
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

  public List<StoreOrderItem> getItems() {
    return items;
  }

  public void addItem(StoreOrderItem item) {
    items.add(item);
    item.setOrder(this);
  }
}
