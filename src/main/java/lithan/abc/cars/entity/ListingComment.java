package lithan.abc.cars.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_listing_comment")
public class ListingComment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_comment")
  private int idComment;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_user", nullable = false)
  private UserAccount author;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_car")
  private Car car;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_part")
  private CarPart part;

  @Column(nullable = false, length = 1000)
  private String body;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "image_file_name")
  private String imageFileName;

  @Column(name = "image_file_type")
  private String imageFileType;

  @Column(name = "image_data", columnDefinition = "LONGTEXT")
  private String imageData;

  public int getIdComment() {
    return idComment;
  }

  public UserAccount getAuthor() {
    return author;
  }

  public void setAuthor(UserAccount author) {
    this.author = author;
  }

  public Car getCar() {
    return car;
  }

  public void setCar(Car car) {
    this.car = car;
  }

  public CarPart getPart() {
    return part;
  }

  public void setPart(CarPart part) {
    this.part = part;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public String getImageFileName() {
    return imageFileName;
  }

  public void setImageFileName(String imageFileName) {
    this.imageFileName = imageFileName;
  }

  public String getImageFileType() {
    return imageFileType;
  }

  public void setImageFileType(String imageFileType) {
    this.imageFileType = imageFileType;
  }

  public String getImageData() {
    return imageData;
  }

  public void setImageData(String imageData) {
    this.imageData = imageData;
  }

  public boolean hasImage() {
    return imageData != null && !imageData.isBlank();
  }
}
