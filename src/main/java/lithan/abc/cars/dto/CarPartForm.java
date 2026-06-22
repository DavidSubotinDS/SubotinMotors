package lithan.abc.cars.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CarPartForm {

  private int idPart;

  @NotBlank
  @Size(max = 80)
  private String sku;

  @NotBlank
  @Size(max = 160)
  private String name;

  @NotBlank
  @Size(max = 80)
  private String category;

  @NotBlank
  @Size(max = 2000)
  private String description;

  @Min(1)
  private long priceMinor;

  @Min(0)
  private int stockQuantity;

  private boolean active = true;

  @Size(max = 500)
  private String imageUrl;

  public int getIdPart() {
    return idPart;
  }

  public void setIdPart(int idPart) {
    this.idPart = idPart;
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
}
