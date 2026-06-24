CREATE TABLE tb_user (
    id_user INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(15) NOT NULL,
    password VARCHAR(255) NOT NULL,
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE TABLE tb_role (
    id_role INT AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(50) NOT NULL,
    id_user INT NOT NULL,
    CONSTRAINT uk_user_role UNIQUE (id_user, role),
    CONSTRAINT fk_role_user FOREIGN KEY (id_user) REFERENCES tb_user(id_user)
);

CREATE TABLE tb_user_profile (
    id_profile INT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(35) NOT NULL,
    last_name VARCHAR(35) NOT NULL,
    phone_number VARCHAR(30) NOT NULL,
    address VARCHAR(255) NOT NULL,
    about VARCHAR(1000),
    id_user INT NOT NULL,
    CONSTRAINT uk_profile_user UNIQUE (id_user),
    CONSTRAINT fk_profile_user FOREIGN KEY (id_user) REFERENCES tb_user(id_user)
);

CREATE TABLE tb_profile_picture (
    id_picture INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    image LONGTEXT NOT NULL,
    id_profile INT NOT NULL,
    CONSTRAINT uk_picture_profile UNIQUE (id_profile),
    CONSTRAINT fk_picture_profile FOREIGN KEY (id_profile) REFERENCES tb_user_profile(id_profile)
);

CREATE TABLE tb_car (
    id_car INT AUTO_INCREMENT PRIMARY KEY,
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    production_year VARCHAR(4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    price INT NOT NULL,
    id_user INT NOT NULL,
    CONSTRAINT fk_car_user FOREIGN KEY (id_user) REFERENCES tb_user(id_user)
);

CREATE TABLE tb_car_picture (
    id_picture INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    image LONGTEXT NOT NULL,
    id_car INT NOT NULL,
    CONSTRAINT uk_picture_car UNIQUE (id_car),
    CONSTRAINT fk_picture_car FOREIGN KEY (id_car) REFERENCES tb_car(id_car)
);

CREATE TABLE tb_car_bid (
    id_bid INT AUTO_INCREMENT PRIMARY KEY,
    bid_price INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    id_user INT NOT NULL,
    id_car INT NOT NULL,
    CONSTRAINT fk_bid_user FOREIGN KEY (id_user) REFERENCES tb_user(id_user),
    CONSTRAINT fk_bid_car FOREIGN KEY (id_car) REFERENCES tb_car(id_car)
);

CREATE TABLE tb_test_drive (
    id_test_drive INT AUTO_INCREMENT PRIMARY KEY,
    test_drive_date DATE NOT NULL,
    id_user INT NOT NULL,
    id_car INT NOT NULL,
    CONSTRAINT uk_test_drive_user_car_date UNIQUE (id_user, id_car, test_drive_date),
    CONSTRAINT fk_test_drive_user FOREIGN KEY (id_user) REFERENCES tb_user(id_user),
    CONSTRAINT fk_test_drive_car FOREIGN KEY (id_car) REFERENCES tb_car(id_car)
);

INSERT INTO tb_user (username, password) VALUES
('admin123', '$2a$12$iDYDnAGZRFOEI0c7DzskH.ErWVIuWuQm.NhTMcs49oheFXr1c27gS'),
('user123', '$2a$10$ZJWpHtDtXsuGgW2Lj.0v6.QBuKX6q2PYp4RRd4483jJIvKltX4/gS');

INSERT INTO tb_role (role, id_user) VALUES
('ROLE_ADMIN', (SELECT id_user FROM tb_user WHERE username = 'admin123')),
('ROLE_USER', (SELECT id_user FROM tb_user WHERE username = 'admin123')),
('ROLE_USER', (SELECT id_user FROM tb_user WHERE username = 'user123'));

INSERT INTO tb_user_profile (first_name, last_name, phone_number, address, about, id_user) VALUES
('Admin', 'Admin', '0123456789', 'Novi Sad', 'Admin account', (SELECT id_user FROM tb_user WHERE username = 'admin123')),
('User', 'User', '0123456789', 'Novi Sad', 'User account', (SELECT id_user FROM tb_user WHERE username = 'user123'));
