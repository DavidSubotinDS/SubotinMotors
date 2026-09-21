CREATE TABLE tb_user (
    id_user INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(15) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(254) NOT NULL,
    CONSTRAINT uk_user_email UNIQUE (email),
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
    address VARCHAR(255),
    street_address VARCHAR(255), city VARCHAR(120), postal_code VARCHAR(30), country VARCHAR(120),
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

CREATE TABLE tb_password_reset_token (
    id_token INT AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP,
    id_user INT NOT NULL,
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (id_user) REFERENCES tb_user(id_user)
);

