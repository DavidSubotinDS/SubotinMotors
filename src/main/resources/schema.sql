-- Drop foreign key constraints
ALTER TABLE tb_profile_picture DROP FOREIGN KEY FK37ogg93fygyvhjeeh0kms3ub9;

-- Drop existing tables
DROP TABLE IF EXISTS tb_role;
DROP TABLE IF EXISTS tb_user_profile;
DROP TABLE IF EXISTS tb_user;

-- Recreate tb_user table
CREATE TABLE tb_user (
    id_user INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(15) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(255)
);

-- Recreate tb_role table
CREATE TABLE tb_role (
    id_role INT AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(255),
    id_user INT,
    FOREIGN KEY (id_user) REFERENCES tb_user(id_user)
);

-- Recreate tb_user_profile table
CREATE TABLE tb_user_profile (
    id_profile INT AUTO_INCREMENT PRIMARY KEY,
    about VARCHAR(255),
    address VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    phone_number VARCHAR(20),
    id_user INT,
    FOREIGN KEY (id_user) REFERENCES tb_user(id_user)
);

-- Recreate tb_car table
CREATE TABLE tb_car (
    id_car INT AUTO_INCREMENT PRIMARY KEY,
    make VARCHAR(255),
    model VARCHAR(255),
    year INT,
    price DECIMAL(10, 2),
    car_picture BLOB,
    id_user INT,
    FOREIGN KEY (id_user) REFERENCES tb_user(id_user)
);

-- Recreate tb_car_picture table
CREATE TABLE tb_car_picture (
    id_car_picture INT AUTO_INCREMENT PRIMARY KEY,
    file_type VARCHAR(255),
    image BLOB,
    id_car INT,
    FOREIGN KEY (id_car) REFERENCES tb_car(id_car)
);

-- Recreate tb_car_bid table
CREATE TABLE tb_car_bid (
    id_car_bid INT AUTO_INCREMENT PRIMARY KEY,
    bid_amount DECIMAL(10, 2),
    status VARCHAR(255),
    id_car INT,
    FOREIGN KEY (id_car) REFERENCES tb_car(id_car)
);

-- Recreate tb_test_drive table
CREATE TABLE tb_test_drive (
    id_test_drive INT AUTO_INCREMENT PRIMARY KEY,
    test_drive_date DATE,
    id_car INT,
    FOREIGN KEY (id_car) REFERENCES tb_car(id_car)
);

-- Recreate tb_profile_picture table
CREATE TABLE tb_profile_picture (
    id_profile_picture INT AUTO_INCREMENT PRIMARY KEY,
    file_type VARCHAR(255),
    image BLOB,
    id_user INT,
    FOREIGN KEY (id_user) REFERENCES tb_user(id_user)
);
