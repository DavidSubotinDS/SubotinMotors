CREATE TABLE tb_delivery_outbox (
  event_id VARCHAR(36) PRIMARY KEY,
  encrypted_payload TEXT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  published_at TIMESTAMP(6) NULL,
  attempts INTEGER NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMP(6) NOT NULL
);
CREATE INDEX ix_delivery_outbox_due ON tb_delivery_outbox(published_at,next_attempt_at);
