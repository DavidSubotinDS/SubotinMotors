CREATE TABLE tb_listing_test_ride (
    id_test_ride INT AUTO_INCREMENT PRIMARY KEY,
    scheduled_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    id_listing INT NOT NULL,
    id_user INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_listing_test_ride_user_time
      UNIQUE (id_listing, id_user, scheduled_at),
    CONSTRAINT fk_listing_test_ride_listing
      FOREIGN KEY (id_listing) REFERENCES tb_car_listing(id_listing),
    CONSTRAINT fk_listing_test_ride_user
      FOREIGN KEY (id_user) REFERENCES tb_user(id_user)
);

CREATE INDEX idx_listing_test_ride_requester
  ON tb_listing_test_ride(id_user, scheduled_at);

CREATE TABLE tb_listing_deposit (
    id_deposit INT AUTO_INCREMENT PRIMARY KEY,
    id_listing INT NOT NULL,
    id_buyer INT NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    checkout_session_id VARCHAR(255),
    checkout_url VARCHAR(2000),
    payment_intent_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_listing_deposit_amount CHECK (amount_minor > 0),
    CONSTRAINT uk_listing_deposit_checkout UNIQUE (checkout_session_id),
    CONSTRAINT uk_listing_deposit_intent UNIQUE (payment_intent_id),
    CONSTRAINT fk_listing_deposit_listing
      FOREIGN KEY (id_listing) REFERENCES tb_car_listing(id_listing),
    CONSTRAINT fk_listing_deposit_buyer
      FOREIGN KEY (id_buyer) REFERENCES tb_user(id_user)
);

CREATE INDEX idx_listing_deposit_buyer
  ON tb_listing_deposit(id_buyer, created_at);

CREATE INDEX idx_listing_deposit_listing_status
  ON tb_listing_deposit(id_listing, status);
