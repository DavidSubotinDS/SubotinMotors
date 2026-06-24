package lithan.autostrada.auctions.entity;

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
@Table(name = "tb_car_listing_picture")
public class CarListingPicture {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_picture")
  private int idPicture;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "file_type", nullable = false, length = 100)
  private String fileType;

  @Column(nullable = false, columnDefinition = "LONGTEXT")
  private String image;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "id_listing", nullable = false, unique = true)
  private CarListing listing;

  public int getIdPicture() {
    return idPicture;
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

  public CarListing getListing() {
    return listing;
  }

  public void setListing(CarListing listing) {
    this.listing = listing;
  }
}
