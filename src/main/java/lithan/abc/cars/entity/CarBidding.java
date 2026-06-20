package lithan.abc.cars.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "tb_car_bid")
public class CarBidding {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_bid")
  private int idBid;

  @Digits(integer = 10, fraction = 2)
  @Positive(message = "Bid price must be positive")
  @Column(nullable = false)
  private int bidPrice;

  private String status;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_user")
  private UserAccount user;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_car")
  private Car car;

  public CarBidding() {
  }

  public int getIdBid() {
    return idBid;
  }

  public void setIdBid(int idBid) {
    this.idBid = idBid;
  }

  public int getBidPrice() {
    return bidPrice;
  }

  public void setBidPrice(int bidPrice) {
    this.bidPrice = bidPrice;
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

  public Car getCar() {
    return car;
  }

  public void setCar(Car car) {
    this.car = car;
  }

}
