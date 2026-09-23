CREATE TABLE tb_notification (
  id_notification INTEGER AUTO_INCREMENT PRIMARY KEY,
  id_user INTEGER NOT NULL,
  id_car INTEGER NOT NULL,
  notification_type VARCHAR(50) NOT NULL,
  message VARCHAR(500) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  read_at TIMESTAMP(6) NULL,
  auction_snapshot TEXT NOT NULL,
  dedupe_key VARCHAR(160) NOT NULL,
  CONSTRAINT uk_notification_business UNIQUE (dedupe_key),
  CONSTRAINT uk_notification_recipient_car_type UNIQUE (id_user,id_car,notification_type),
  CONSTRAINT ck_notification_ids CHECK (id_user>0 AND id_car>0)
);
CREATE INDEX ix_notification_recipient ON tb_notification(id_user,created_at,id_notification);
CREATE TABLE tb_message_inbox (
  consumer_name VARCHAR(80) NOT NULL,
  event_id VARCHAR(36) NOT NULL,
  received_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (consumer_name,event_id)
);
CREATE TABLE tb_delivery (
  delivery_id VARCHAR(36) PRIMARY KEY,
  encrypted_payload TEXT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  state VARCHAR(20) NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMP(6) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL
);
CREATE INDEX ix_delivery_due ON tb_delivery(state,next_attempt_at);
