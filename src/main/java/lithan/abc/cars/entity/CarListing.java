package lithan.abc.cars.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "tb_car_listing")
public class CarListing {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_listing")
  private int idListing;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, length = 100)
  private String make;

  @Column(nullable = false, length = 100)
  private String model;

  @Column(name = "production_year", nullable = false, length = 4)
  private String year;

  @Column(nullable = false)
  private int mileage;

  @Column(name = "fuel_type", nullable = false, length = 40)
  private String fuelType;

  @Column(nullable = false, length = 40)
  private String transmission;

  @Column(name = "price_minor", nullable = false)
  private long priceMinor;

  @Column(name = "deposit_amount_minor", nullable = false)
  private long depositAmountMinor;

  @Column(nullable = false, length = 3000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private CarListingStatus status = CarListingStatus.ACTIVE;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_seller", nullable = false)
  private UserAccount seller;

  @OneToOne(
      fetch = FetchType.EAGER,
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      mappedBy = "listing")
  private CarListingPicture picture;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  private long version;

  public int getIdListing() {
    return idListing;
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

  public int getMileage() {
    return mileage;
  }

  public void setMileage(int mileage) {
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

  public long getPriceMinor() {
    return priceMinor;
  }

  public void setPriceMinor(long priceMinor) {
    this.priceMinor = priceMinor;
  }

  public BigDecimal getPriceAmount() {
    return BigDecimal.valueOf(priceMinor, 2);
  }

  public long getDepositAmountMinor() {
    return depositAmountMinor;
  }

  public void setDepositAmountMinor(long depositAmountMinor) {
    this.depositAmountMinor = depositAmountMinor;
  }

  public BigDecimal getDepositAmount() {
    return BigDecimal.valueOf(depositAmountMinor, 2);
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public CarListingStatus getStatus() {
    return status;
  }

  public void setStatus(CarListingStatus status) {
    this.status = status;
  }

  public boolean isActive() {
    return status == CarListingStatus.ACTIVE;
  }

  public boolean isReserved() {
    return status == CarListingStatus.RESERVED;
  }

  public UserAccount getSeller() {
    return seller;
  }

  public void setSeller(UserAccount seller) {
    this.seller = seller;
  }

  public CarListingPicture getPicture() {
    return picture;
  }

  public void setPicture(CarListingPicture picture) {
    this.picture = picture;
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
