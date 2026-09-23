CREATE TABLE tb_notification_outbox (
  event_id VARCHAR(36) PRIMARY KEY,
  dedupe_key VARCHAR(160) NOT NULL UNIQUE,
  payload TEXT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  published_at TIMESTAMP(6) NULL,
  attempts INTEGER NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMP(6) NOT NULL
);
CREATE INDEX ix_notification_outbox_due ON tb_notification_outbox(published_at,next_attempt_at);
