package lithan.autostrada.auctions.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "tb_auction_follow",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_auction_follow_user_car",
        columnNames = {"id_user", "id_car"}))
public class AuctionFollow {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_follow")
  private int idFollow;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_user")
  private UserAccount user;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "id_car")
  private Car car;

  @Column(name = "followed_at", nullable = false)
  private LocalDateTime followedAt;

  public int getIdFollow() {
    return idFollow;
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

  public LocalDateTime getFollowedAt() {
    return followedAt;
  }

  public void setFollowedAt(LocalDateTime followedAt) {
    this.followedAt = followedAt;
  }
}
