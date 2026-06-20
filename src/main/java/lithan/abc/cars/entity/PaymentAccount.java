package lithan.abc.cars.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_payment_account")
public class PaymentAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_payment_account")
  private int idPaymentAccount;

  @OneToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_user", nullable = false, unique = true)
  private UserAccount user;

  @Column(name = "provider_account_id", nullable = false, unique = true, length = 255)
  private String providerAccountId;

  @Column(nullable = false, length = 30)
  private String status;

  @Column(name = "transfers_enabled", nullable = false)
  private boolean transfersEnabled;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public int getIdPaymentAccount() {
    return idPaymentAccount;
  }

  public UserAccount getUser() {
    return user;
  }

  public void setUser(UserAccount user) {
    this.user = user;
  }

  public String getProviderAccountId() {
    return providerAccountId;
  }

  public void setProviderAccountId(String providerAccountId) {
    this.providerAccountId = providerAccountId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public boolean isTransfersEnabled() {
    return transfersEnabled;
  }

  public void setTransfersEnabled(boolean transfersEnabled) {
    this.transfersEnabled = transfersEnabled;
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
}
