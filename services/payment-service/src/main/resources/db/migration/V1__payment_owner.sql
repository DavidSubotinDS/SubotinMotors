CREATE TABLE payment_attempt (
  payment_id VARCHAR(36) PRIMARY KEY,
  attempt_id VARCHAR(36) NOT NULL,
  source_service VARCHAR(40) NOT NULL,
  business_type VARCHAR(40) NOT NULL,
  business_id VARCHAR(80) NOT NULL,
  business_version BIGINT NOT NULL,
  buyer_id VARCHAR(80) NOT NULL,
  amount_minor BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL,
  description VARCHAR(255) NOT NULL,
  return_route VARCHAR(40) NOT NULL,
  customer_email VARCHAR(254),
  request_hash CHAR(64) NOT NULL,
  status VARCHAR(40) NOT NULL,
  provider_session_id VARCHAR(255),
  provider_payment_intent_id VARCHAR(255),
  checkout_url VARCHAR(2000),
  failure_count INT NOT NULL DEFAULT 0,
  last_failure_code VARCHAR(80),
  next_reconcile_at TIMESTAMP NULL,
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  aggregate_version BIGINT NOT NULL DEFAULT 1,
  CONSTRAINT uk_payment_attempt_attempt UNIQUE (attempt_id),
  CONSTRAINT uk_payment_attempt_business UNIQUE (source_service,business_type,business_id,attempt_id),
  CONSTRAINT uk_payment_attempt_session UNIQUE (provider_session_id),
  CONSTRAINT uk_payment_attempt_intent UNIQUE (provider_payment_intent_id),
  CONSTRAINT ck_payment_amount CHECK (amount_minor > 0),
  CONSTRAINT ck_payment_failure_count CHECK (failure_count >= 0),
  CONSTRAINT ck_payment_source_purpose CHECK (
    (source_service='commerce-service' AND business_type='STORE_ORDER') OR
    (source_service='marketplace-service' AND business_type='LISTING_DEPOSIT') OR
    (source_service='legacy-backend' AND business_type='AUCTION_PURCHASE'))
);
CREATE INDEX idx_payment_attempt_reconcile ON payment_attempt(status,next_reconcile_at);
CREATE INDEX idx_payment_attempt_business ON payment_attempt(source_service,business_type,business_id);

CREATE TABLE payment_webhook_receipt (
  receipt_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  provider_event_id VARCHAR(255) NOT NULL,
  event_type VARCHAR(255) NOT NULL,
  provider_session_id VARCHAR(255),
  provider_payment_intent_id VARCHAR(255),
  payment_status VARCHAR(40),
  payload_hash CHAR(64) NOT NULL,
  status VARCHAR(40) NOT NULL,
  delivery_count INT NOT NULL DEFAULT 1,
  failure_code VARCHAR(80),
  received_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  processed_at TIMESTAMP NULL,
  CONSTRAINT uk_payment_receipt_event UNIQUE (provider_event_id),
  CONSTRAINT ck_payment_receipt_delivery CHECK (delivery_count > 0)
);
CREATE INDEX idx_payment_receipt_pending ON payment_webhook_receipt(status,received_at);

CREATE TABLE payment_outbox (
  event_id VARCHAR(36) PRIMARY KEY,
  aggregate_id VARCHAR(36) NOT NULL,
  aggregate_version BIGINT NOT NULL,
  routing_key VARCHAR(80) NOT NULL,
  payload_json TEXT NOT NULL,
  correlation_id VARCHAR(80) NOT NULL,
  causation_id VARCHAR(255),
  status VARCHAR(20) NOT NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL,
  published_at TIMESTAMP NULL,
  CONSTRAINT uk_payment_outbox_aggregate UNIQUE (aggregate_id,aggregate_version,routing_key),
  CONSTRAINT ck_payment_outbox_attempts CHECK (attempt_count >= 0)
);
CREATE INDEX idx_payment_outbox_pending ON payment_outbox(status,created_at);

CREATE TABLE payment_provider_account_audit (
  original_id INT PRIMARY KEY,
  user_id INT NOT NULL,
  provider_account_id VARCHAR(255) NOT NULL,
  status VARCHAR(30) NOT NULL,
  transfers_enabled BOOLEAN NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  CONSTRAINT uk_payment_account_audit_user UNIQUE (user_id),
  CONSTRAINT uk_payment_account_audit_provider UNIQUE (provider_account_id)
);

CREATE TABLE payment_legacy_audit (
  original_id INT PRIMARY KEY,
  bid_id INT NOT NULL,
  buyer_id INT NOT NULL,
  seller_id INT NOT NULL,
  amount_minor BIGINT NOT NULL,
  platform_fee_minor BIGINT NOT NULL,
  currency VARCHAR(3) NOT NULL,
  status VARCHAR(30) NOT NULL,
  purpose VARCHAR(30) NOT NULL,
  provider_session_id VARCHAR(255),
  provider_payment_intent_id VARCHAR(255),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  paid_at TIMESTAMP NULL,
  version BIGINT NOT NULL,
  CONSTRAINT uk_payment_legacy_bid UNIQUE (bid_id)
);

CREATE TABLE payment_copy_checkpoint (
  id INT PRIMARY KEY,
  state VARCHAR(40) NOT NULL,
  source_fingerprint VARCHAR(128),
  verified_at TIMESTAMP NULL,
  updated_at TIMESTAMP NOT NULL
);
INSERT INTO payment_copy_checkpoint(id,state,updated_at) VALUES (1,'NOT_STARTED',CURRENT_TIMESTAMP);
