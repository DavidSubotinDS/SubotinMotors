package lithan.abc.cars.entity;

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
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lithan.abc.cars.validation.ProductionYear;

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
