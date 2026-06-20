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

@Entity
@Table(name = "tb_profile_picture")
public class ProfilePicture {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_picture")
  private int idPicture;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "file_type", nullable = false)
  private String fileType;

  @Column(nullable = false, columnDefinition = "LONGTEXT")
  private String image;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_profile")
  private UserProfile profile;

  public ProfilePicture() {
  }

  public ProfilePicture(String fileName, String fileType, String image, UserProfile profile) {
    this.fileName = fileName;
    this.fileType = fileType;
    this.image = image;
    this.profile = profile;
  }

  public int getIdPicture() {
    return idPicture;
  }

  public void setIdPicture(int idPicture) {
    this.idPicture = idPicture;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public UserProfile getProfile() {
    return profile;
  }

  public void setProfile(UserProfile profile) {
    this.profile = profile;
  }

}
