package lithan.abc.cars.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lithan.abc.cars.entity.UserAccount;
import lithan.abc.cars.entity.UserProfile;

public class UserProfileForm {

  private int idProfile;

  @NotEmpty(message = "Email is required")
  @Email(message = "Enter a valid email address")
  @Size(max = 254, message = "Email must not exceed 254 characters")
  private String email;

  @NotEmpty(message = "First name is required")
  @Size(min = 1, max = 35, message = "First name must be between 1 and 35 characters long")
  private String firstName;

  @NotEmpty(message = "Last name is required")
  @Size(min = 1, max = 35, message = "Last Name must be between 1 and 35 characters long")
  private String lastName;

  @NotEmpty(message = "Phone number is required")
  @Pattern(
      regexp = "^(?:\\+[1-9]\\d{7,14}|\\d{8,15})$",
      message = "Phone number must contain 8 to 15 digits, with an optional leading +")
  private String phoneNumber;

  @NotEmpty(message = "Address is required")
  private String address;

  @Size(max = 1000, message = "About must not exceed 1000 characters")
  private String about;

  public static UserProfileForm from(UserAccount user) {
    UserProfile profile = user.getProfile();
    UserProfileForm form = new UserProfileForm();
    form.setIdProfile(profile.getIdProfile());
    form.setEmail(user.getEmail());
    form.setFirstName(profile.getFirstName());
    form.setLastName(profile.getLastName());
    form.setPhoneNumber(profile.getPhoneNumber());
    form.setAddress(profile.getAddress());
    form.setAbout(profile.getAbout());
    return form;
  }

  public int getIdProfile() {
    return idProfile;
  }

  public void setIdProfile(int idProfile) {
    this.idProfile = idProfile;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
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

  public String getAbout() {
    return about;
  }

  public void setAbout(String about) {
    this.about = about;
  }
}
