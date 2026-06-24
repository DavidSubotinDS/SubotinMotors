package lithan.autostrada.auctions.entity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import lithan.autostrada.auctions.validation.ProductionYear;

@Entity
@Table(name = "tb_car")
public class Car {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_car")
  private int idCar;

  @NotBlank(message = "Make is required")
  @Column(nullable = false)
  private String make;

  @NotBlank(message = "Model is required")
  @Column(nullable = false)
  private String model;

  @ProductionYear
  @Column(name = "production_year", nullable = false)
  private String year;

  @Column(nullable = false)
  private String status;

  @Column(nullable = false)
  @Digits(integer = 10, fraction = 2)
  @Positive(message = "Price can't below 0 or Negative number")
  private int price;

  @NotNull(message = "Auction end date and time is required")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  @Column(name = "auction_end_time", nullable = false)
  private LocalDateTime auctionEndTime =
      LocalDateTime.now().plusDays(7).withSecond(0).withNano(0);

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_user")
  private UserAccount user;

  @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "car")
  private CarPicture carPicture;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "car")
  private List<CarBidding> carBiddings;

  public Car() {
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

  public int getPrice() {
    return price;
  }

  public void setPrice(int price) {
    this.price = price;
  }

  public LocalDateTime getAuctionEndTime() {
    return auctionEndTime;
  }

  public void setAuctionEndTime(LocalDateTime auctionEndTime) {
    this.auctionEndTime = auctionEndTime;
  }

  @Transient
  public boolean isAuctionOpen() {
    return isAuctionOpenAt(LocalDateTime.now());
  }

  public boolean isAuctionOpenAt(LocalDateTime now) {
    return "ACTIVE".equals(status)
        && auctionEndTime != null
        && auctionEndTime.isAfter(now);
  }

  @Transient
  public String getAuctionStatus() {
    return auctionStatusAt(LocalDateTime.now());
  }

  public String auctionStatusAt(LocalDateTime now) {
    if ("SOLD".equals(status)) {
      return "SOLD";
    }
    if ("ACTIVE".equals(status) && auctionEndTime != null && !auctionEndTime.isAfter(now)) {
      return "ENDED";
    }
    if ("ACTIVE".equals(status)
        && auctionEndTime != null
        && !auctionEndTime.isAfter(now.plusHours(24))) {
      return "ENDING_SOON";
    }
    return status;
  }

  @Transient
  public String getAuctionStatusLabel() {
    return switch (getAuctionStatus()) {
      case "ENDING_SOON" -> "Ending soon";
      case "ENDED" -> "Ended";
      case "SOLD" -> "Sold";
      case "ACTIVE" -> "Active";
      case "PENDING" -> "Pending approval";
      case "DEACTIVE" -> "Inactive";
      case "RESERVED" -> "Reserved";
      default -> status;
    };
  }

  @Transient
  public long getAuctionEndTimeEpochMillis() {
    if (auctionEndTime == null) {
      return 0;
    }
    return auctionEndTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
  }

  @Transient
  public String getAuctionEndTimeDisplay() {
    if (auctionEndTime == null) {
      return "Not scheduled";
    }
    return auctionEndTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"));
  }

  public boolean isEndingWithin(Duration duration, LocalDateTime now) {
    return isAuctionOpenAt(now) && !auctionEndTime.isAfter(now.plus(duration));
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public UserAccount getUser() {
    return user;
  }

  public void setUser(UserAccount user) {
    this.user = user;
  }

  public CarPicture getCarPicture() {
    return carPicture;
  }

  public void setCarPicture(CarPicture carPicture) {
    this.carPicture = carPicture;
  }

  public int getIdCar() {
    return idCar;
  }

  public void setIdCar(int idCar) {
    this.idCar = idCar;
  }

  public List<CarBidding> getCarBiddings() {
    return carBiddings;
  }

  public void setCarBiddings(List<CarBidding> carBiddings) {
    this.carBiddings = carBiddings;
  }

}
