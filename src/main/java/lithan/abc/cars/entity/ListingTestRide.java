package lithan.abc.cars.entity;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_listing_test_ride")
public class ListingTestRide {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_test_ride")
  private int idTestRide;

  @Column(name = "scheduled_at", nullable = false)
  private LocalDateTime scheduledAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TestDriveStatus status = TestDriveStatus.PENDING;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_listing", nullable = false)
  private CarListing listing;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_user", nullable = false)
  private UserAccount user;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public int getIdTestRide() {
    return idTestRide;
  }

  public LocalDateTime getScheduledAt() {
    return scheduledAt;
  }

  public void setScheduledAt(LocalDateTime scheduledAt) {
    this.scheduledAt = scheduledAt;
  }

  public TestDriveStatus getStatus() {
    return status;
  }

  public void setStatus(TestDriveStatus status) {
    this.status = status;
  }

  public boolean isPending() {
    return status == TestDriveStatus.PENDING;
  }

  public boolean isAccepted() {
    return status == TestDriveStatus.ACCEPTED;
  }

  public boolean isRejected() {
    return status == TestDriveStatus.REJECTED;
  }

  public boolean isReschedulable() {
    return isPending() || isAccepted();
  }

  public boolean isCancellable() {
    return isReschedulable();
  }

  public CarListing getListing() {
    return listing;
  }

  public void setListing(CarListing listing) {
    this.listing = listing;
  }

  public UserAccount getUser() {
    return user;
  }

  public void setUser(UserAccount user) {
    this.user = user;
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
