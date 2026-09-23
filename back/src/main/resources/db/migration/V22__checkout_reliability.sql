CREATE TABLE tb_checkout_attempt (
    attempt_id VARCHAR(36) PRIMARY KEY,
    id_user INT NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    client_request_id VARCHAR(100) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    customer_email VARCHAR(254) NOT NULL,
    aggregate_id INT,
    status VARCHAR(30) NOT NULL,
    provider_session_id VARCHAR(255),
    provider_checkout_url VARCHAR(2000),
    payment_intent_id VARCHAR(255),
    failure_count INT NOT NULL DEFAULT 0,
    last_failure_code VARCHAR(80),
    next_reconcile_at TIMESTAMP NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_checkout_attempt_request UNIQUE (id_user, purpose, client_request_id),
    CONSTRAINT uk_checkout_attempt_session UNIQUE (provider_session_id),
    CONSTRAINT ck_checkout_attempt_purpose CHECK (purpose IN ('STORE_ORDER', 'LISTING_DEPOSIT')),
    CONSTRAINT ck_checkout_attempt_failure_count CHECK (failure_count >= 0)
);

CREATE INDEX idx_checkout_attempt_reconcile
  ON tb_checkout_attempt(status, next_reconcile_at);
CREATE INDEX idx_checkout_attempt_age
  ON tb_checkout_attempt(status, created_at);

ALTER TABLE tb_store_order ADD COLUMN checkout_attempt_id VARCHAR(36);
ALTER TABLE tb_store_order
  ADD CONSTRAINT uk_store_order_attempt UNIQUE (checkout_attempt_id),
  ADD CONSTRAINT fk_store_order_attempt FOREIGN KEY (checkout_attempt_id)
    REFERENCES tb_checkout_attempt(attempt_id);

ALTER TABLE tb_listing_deposit ADD COLUMN checkout_attempt_id VARCHAR(36);
ALTER TABLE tb_listing_deposit
  ADD CONSTRAINT uk_listing_deposit_attempt UNIQUE (checkout_attempt_id),
  ADD CONSTRAINT fk_listing_deposit_attempt FOREIGN KEY (checkout_attempt_id)
    REFERENCES tb_checkout_attempt(attempt_id);

CREATE TABLE tb_stock_hold (
    id_stock_hold BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id VARCHAR(36) NOT NULL,
    id_part INT NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP NULL,
    CONSTRAINT uk_stock_hold_attempt_part UNIQUE (attempt_id, id_part),
    CONSTRAINT ck_stock_hold_quantity CHECK (quantity > 0),
    CONSTRAINT ck_stock_hold_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED')),
    CONSTRAINT fk_stock_hold_attempt FOREIGN KEY (attempt_id)
      REFERENCES tb_checkout_attempt(attempt_id),
    CONSTRAINT fk_stock_hold_part FOREIGN KEY (id_part)
      REFERENCES tb_car_part(id_part)
);

CREATE INDEX idx_stock_hold_status ON tb_stock_hold(status, created_at);

CREATE TABLE tb_checkout_webhook_inbox (
    id_inbox BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    checkout_session_id VARCHAR(255),
    payment_intent_id VARCHAR(255),
    payment_status VARCHAR(40),
    status VARCHAR(20) NOT NULL,
    delivery_count INT NOT NULL DEFAULT 1,
    received_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    CONSTRAINT uk_checkout_webhook_inbox_event UNIQUE (provider_event_id),
    CONSTRAINT ck_checkout_webhook_inbox_status CHECK (status IN ('PENDING', 'PROCESSED'))
);

CREATE INDEX idx_checkout_webhook_pending
  ON tb_checkout_webhook_inbox(status, received_at);
