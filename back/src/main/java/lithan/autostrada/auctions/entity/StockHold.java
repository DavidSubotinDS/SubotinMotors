package lithan.autostrada.auctions.entity;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "tb_stock_hold")
public class StockHold {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_stock_hold")
  private long idStockHold;
  @Column(name = "attempt_id", nullable = false, length = 36)
  private String attemptId;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_part", nullable = false)
  private CarPart part;
  @Column(nullable = false)
  private int quantity;
  @Column(nullable = false, length = 20)
  private String status;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;
  @Column(name = "released_at")
  private Instant releasedAt;

  public long getIdStockHold() { return idStockHold; }
  public String getAttemptId() { return attemptId; }
  public void setAttemptId(String value) { attemptId = value; }
  public CarPart getPart() { return part; }
  public void setPart(CarPart value) { part = value; }
  public int getQuantity() { return quantity; }
  public void setQuantity(int value) { quantity = value; }
  public String getStatus() { return status; }
  public void setStatus(String value) { status = value; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant value) { createdAt = value; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant value) { updatedAt = value; }
  public Instant getReleasedAt() { return releasedAt; }
  public void setReleasedAt(Instant value) { releasedAt = value; }
}

