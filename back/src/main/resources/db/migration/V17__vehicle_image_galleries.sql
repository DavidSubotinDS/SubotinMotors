CREATE TABLE tb_car_gallery_picture (
    id_picture INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    image LONGTEXT NOT NULL,
    display_order INT NOT NULL,
    id_car INT NOT NULL,
    CONSTRAINT fk_car_gallery_picture
      FOREIGN KEY (id_car) REFERENCES tb_car(id_car)
);

CREATE INDEX idx_car_gallery_picture_order
  ON tb_car_gallery_picture(id_car, display_order, id_picture);

CREATE TABLE tb_car_listing_gallery_picture (
    id_picture INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    image LONGTEXT NOT NULL,
    display_order INT NOT NULL,
    id_listing INT NOT NULL,
    CONSTRAINT fk_car_listing_gallery_picture
      FOREIGN KEY (id_listing) REFERENCES tb_car_listing(id_listing)
);

CREATE INDEX idx_car_listing_gallery_picture_order
  ON tb_car_listing_gallery_picture(id_listing, display_order, id_picture);
