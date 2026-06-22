package lithan.abc.cars.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tb_user")
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_user")
  private int idUser;

  @NotEmpty(message = "Username is required")
  @Size(min = 3, max = 15, message = "Username must be between 3 and 15 characters long")
  @Column(nullable = false)
  private String username;

  @NotEmpty(message = "Email is required")
  @Email(message = "Enter a valid email address")
  @Size(max = 254, message = "Email must not exceed 254 characters")
  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @NotEmpty(message = "Password is required")
  @Size(min = 6, message = "Password must be greater or equal to 6")
  @Column(nullable = false)
  private String password;

  @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "user")
  private UserProfile profile;

  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "user")
  private List<Role> roles;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "user")
  private List<Car> cars;

  @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "user")
  private List<CarBidding> carBiddings;

  public UserAccount() {
  }

  public UserAccount(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public int getIdUser() {
    return idUser;
  }

  public void setIdUser(int idUser) {
    this.idUser = idUser;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public List<Role> getRoles() {
    return roles;
  }

  public void setRoles(List<Role> roles) {
    this.roles = roles;
  }

  public UserProfile getProfile() {
    return profile;
  }

  public void setProfile(UserProfile profile) {
    this.profile = profile;
  }

  public List<Car> getCars() {
    return cars;
  }

  public void setCars(List<Car> cars) {
    this.cars = cars;
  }

  public List<CarBidding> getCarBiddings() {
    return carBiddings;
  }

  public void setCarBiddings(List<CarBidding> carBiddings) {
    this.carBiddings = carBiddings;
  }

}
