package lithan.autostrada.auctions.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lithan.autostrada.auctions.entity.CarListing;
import lithan.autostrada.auctions.validation.ProductionYear;

public class CarListingForm {

  private int idListing;

  @NotBlank(message = "Title is required")
  @Size(max = 160, message = "Title must not exceed 160 characters")
  private String title;

  @NotBlank(message = "Make is required")
  @Size(max = 100, message = "Make must not exceed 100 characters")
  private String make;

  @NotBlank(message = "Model is required")
  @Size(max = 100, message = "Model must not exceed 100 characters")
  private String model;

  @ProductionYear
  private String year;

  @NotNull(message = "Mileage is required")
  @PositiveOrZero(message = "Mileage cannot be negative")
  private Integer mileage;

  @NotBlank(message = "Fuel type is required")
  @Size(max = 40, message = "Fuel type must not exceed 40 characters")
  private String fuelType;

  @NotBlank(message = "Transmission is required")
  @Size(max = 40, message = "Transmission must not exceed 40 characters")
  private String transmission;

  @NotNull(message = "Price is required")
  @DecimalMin(value = "0.01", message = "Price must be positive")
  @Digits(integer = 10, fraction = 2, message = "Price can have at most two decimal places")
  private BigDecimal price;

  @NotNull(message = "Deposit amount is required")
  @DecimalMin(value = "0.01", message = "Deposit amount must be positive")
  @Digits(integer = 10, fraction = 2, message = "Deposit can have at most two decimal places")
  private BigDecimal depositAmount;

  @NotBlank(message = "Description is required")
  @Size(max = 3000, message = "Description must not exceed 3000 characters")
  private String description;

  public static CarListingForm from(CarListing listing) {
    CarListingForm form = new CarListingForm();
    form.setIdListing(listing.getIdListing());
    form.setTitle(listing.getTitle());
    form.setMake(listing.getMake());
    form.setModel(listing.getModel());
    form.setYear(listing.getYear());
    form.setMileage(listing.getMileage());
    form.setFuelType(listing.getFuelType());
    form.setTransmission(listing.getTransmission());
    form.setPrice(listing.getPriceAmount());
    form.setDepositAmount(listing.getDepositAmount());
    form.setDescription(listing.getDescription());
    return form;
  }

  @AssertTrue(message = "Deposit amount must be less than the listed price")
  public boolean isDepositLessThanPrice() {
    return price == null || depositAmount == null || depositAmount.compareTo(price) < 0;
  }

  public int getIdListing() {
    return idListing;
  }

  public void setIdListing(int idListing) {
    this.idListing = idListing;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getMake() {
    return make;
  }

  public void setMake(String make) {
    this.make = make;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  public Integer getMileage() {
    return mileage;
  }

  public void setMileage(Integer mileage) {
    this.mileage = mileage;
  }

  public String getFuelType() {
    return fuelType;
  }

  public void setFuelType(String fuelType) {
    this.fuelType = fuelType;
  }

  public String getTransmission() {
    return transmission;
  }

  public void setTransmission(String transmission) {
    this.transmission = transmission;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public BigDecimal getDepositAmount() {
    return depositAmount;
  }

  public void setDepositAmount(BigDecimal depositAmount) {
    this.depositAmount = depositAmount;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
