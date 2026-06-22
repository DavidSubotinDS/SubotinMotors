package lithan.abc.cars.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tb_car_part")
public class CarPart {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_part")
  private int idPart;

  @NotBlank
  @Size(max = 80)
  @Column(nullable = false, unique = true, length = 80)
  private String sku;

  @NotBlank
  @Size(max = 160)
  @Column(nullable = false, length = 160)
  private String name;

  @NotBlank
  @Size(max = 80)
  @Column(nullable = false, length = 80)
  private String category;

  @NotBlank
  @Size(max = 2000)
  @Column(nullable = false, length = 2000)
  private String description;

  @Min(1)
  @Column(name = "price_minor", nullable = false)
  private long priceMinor;

  @Min(0)
  @Column(name = "stock_quantity", nullable = false)
  private int stockQuantity;

  @Column(nullable = false)
  private boolean active = true;

  @Size(max = 500)
  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  private long version;

  public int getIdPart() {
    return idPart;
  }

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getPriceMinor() {
    return priceMinor;
  }

  public void setPriceMinor(long priceMinor) {
    this.priceMinor = priceMinor;
  }

  public int getStockQuantity() {
    return stockQuantity;
  }

  public void setStockQuantity(int stockQuantity) {
    this.stockQuantity = stockQuantity;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
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
