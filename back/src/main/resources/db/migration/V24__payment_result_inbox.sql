CREATE TABLE tb_payment_result_inbox (
  event_id VARCHAR(36) PRIMARY KEY,
  event_type VARCHAR(80) NOT NULL,
  attempt_id VARCHAR(36) NOT NULL,
  status VARCHAR(30) NOT NULL,
  received_at TIMESTAMP NOT NULL,
  processed_at TIMESTAMP NULL,
  failure_code VARCHAR(80),
  CONSTRAINT fk_payment_result_attempt FOREIGN KEY (attempt_id) REFERENCES tb_checkout_attempt(attempt_id)
);
CREATE INDEX idx_payment_result_status ON tb_payment_result_inbox(status,received_at);
