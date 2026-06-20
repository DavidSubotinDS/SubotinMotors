CREATE TABLE tb_payment_account (
    id_payment_account INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT NOT NULL,
    provider_account_id VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    transfers_enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_payment_account_user UNIQUE (id_user),
    CONSTRAINT uk_payment_account_provider UNIQUE (provider_account_id),
    CONSTRAINT fk_payment_account_user FOREIGN KEY (id_user) REFERENCES tb_user(id_user)
);

CREATE TABLE tb_payment_order (
    id_payment INT AUTO_INCREMENT PRIMARY KEY,
    id_bid INT NOT NULL,
    id_buyer INT NOT NULL,
    id_seller INT NOT NULL,
    amount_minor BIGINT NOT NULL,
    platform_fee_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    checkout_session_id VARCHAR(255),
    checkout_url VARCHAR(2000),
    payment_intent_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_payment_order_bid UNIQUE (id_bid),
    CONSTRAINT uk_payment_order_checkout UNIQUE (checkout_session_id),
    CONSTRAINT uk_payment_order_intent UNIQUE (payment_intent_id),
    CONSTRAINT fk_payment_order_bid FOREIGN KEY (id_bid) REFERENCES tb_car_bid(id_bid),
    CONSTRAINT fk_payment_order_buyer FOREIGN KEY (id_buyer) REFERENCES tb_user(id_user),
    CONSTRAINT fk_payment_order_seller FOREIGN KEY (id_seller) REFERENCES tb_user(id_user)
);

CREATE TABLE tb_payment_webhook_event (
    id_webhook_event INT AUTO_INCREMENT PRIMARY KEY,
    provider_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_payment_webhook_provider_event UNIQUE (provider_event_id)
);
