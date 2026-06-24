CREATE TABLE tb_car_listing (
    id_listing INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    production_year VARCHAR(4) NOT NULL,
    mileage INT NOT NULL,
    fuel_type VARCHAR(40) NOT NULL,
    transmission VARCHAR(40) NOT NULL,
    price_minor BIGINT NOT NULL,
    deposit_amount_minor BIGINT NOT NULL,
    description VARCHAR(3000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    id_seller INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_car_listing_mileage CHECK (mileage >= 0),
    CONSTRAINT ck_car_listing_price CHECK (price_minor > 0),
    CONSTRAINT ck_car_listing_deposit CHECK (
      deposit_amount_minor > 0 AND deposit_amount_minor < price_minor
    ),
    CONSTRAINT fk_car_listing_seller
      FOREIGN KEY (id_seller) REFERENCES tb_user(id_user)
);

CREATE INDEX idx_car_listing_status_created
  ON tb_car_listing(status, created_at);

CREATE INDEX idx_car_listing_seller
  ON tb_car_listing(id_seller, created_at);

CREATE TABLE tb_car_listing_picture (
    id_picture INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    image LONGTEXT NOT NULL,
    id_listing INT NOT NULL,
    CONSTRAINT uk_car_listing_picture UNIQUE (id_listing),
    CONSTRAINT fk_car_listing_picture
      FOREIGN KEY (id_listing) REFERENCES tb_car_listing(id_listing)
);
