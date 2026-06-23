package lithan.abc.cars.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "tb_user_profile")
public class UserProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_profile")
  private int idProfile;

  @Column(name = "first_name", nullable = false)
  @NotEmpty(message = "First name is required")
  @Size(min = 1, max = 35, message = "First name must be between 1 and 35 characters long")
  private String firstName;

  @Column(name = "last_name", nullable = false)
  @NotEmpty(message = "Last name is required")
  @Size(min = 1, max = 35, message = "Last Name must be between 1 and 35 characters long")
  private String lastName;

  @Column(name = "phone_number", nullable = false)
  @NotEmpty(message = "Phone number is required")
  @Pattern(
      regexp = "^(?:\\+[1-9]\\d{7,14}|\\d{8,15})$",
      message = "Phone number must contain 8 to 15 digits, with an optional leading +")
  private String phoneNumber;

  @Column
  private String address;

  @Column(name = "street_address")
  @Size(max = 255, message = "Street address must not exceed 255 characters")
  private String streetAddress;

  @Column(length = 120)
  @Size(max = 120, message = "City must not exceed 120 characters")
  private String city;

  @Column(name = "postal_code", length = 30)
  @Size(max = 30, message = "Postal code must not exceed 30 characters")
  private String postalCode;

  @Column(length = 120)
  @Size(max = 120, message = "Country must not exceed 120 characters")
  private String country;

  private String about;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_user")
  private UserAccount user;

  @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "profile")
  private ProfilePicture profilePicture;

  public UserProfile() {
  }

  public UserProfile(String firstName, String lastName, String phoneNumber, String address,
      String about, UserAccount user, ProfilePicture profilePicture) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.phoneNumber = phoneNumber;
    this.address = address;
    this.about = about;
    this.user = user;
    this.profilePicture = profilePicture;
  }

  public int getIdProfile() {
    return idProfile;
  }

  public void setIdProfile(int idProfile) {
    this.idProfile = idProfile;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getStreetAddress() {
    return streetAddress;
  }

  public void setStreetAddress(String streetAddress) {
    this.streetAddress = streetAddress;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  @AssertTrue(message = "Complete all shipping address fields or leave all of them blank")
  public boolean isPhysicalAddressConsistent() {
    int completed = 0;
    completed += hasText(streetAddress) ? 1 : 0;
    completed += hasText(city) ? 1 : 0;
    completed += hasText(postalCode) ? 1 : 0;
    completed += hasText(country) ? 1 : 0;
    return completed == 0 || completed == 4;
  }

  public boolean hasCompleteShippingAddress() {
    return hasText(streetAddress)
        && hasText(city)
        && hasText(postalCode)
        && hasText(country);
  }

  public boolean isCompleteShippingAddress() {
    return hasCompleteShippingAddress();
  }

  public String getFormattedShippingAddress() {
    if (!hasCompleteShippingAddress()) {
      return "";
    }
    return streetAddress.trim() + ", " + postalCode.trim() + " " + city.trim()
        + ", " + country.trim();
  }

  public String getDisplayLocation() {
    if (hasText(city) && hasText(country)) {
      return city.trim() + ", " + country.trim();
    }
    return address == null ? "" : address;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  public String getAbout() {
    return about;
  }

  public void setAbout(String about) {
    this.about = about;
  }

  public UserAccount getUser() {
    return user;
  }

  public void setUser(UserAccount user) {
    this.user = user;
  }

  public ProfilePicture getProfilePicture() {
    return profilePicture;
  }

  public void setProfilePicture(ProfilePicture profilePicture) {
    this.profilePicture = profilePicture;
  }

}
