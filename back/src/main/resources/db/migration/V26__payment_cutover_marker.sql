CREATE TABLE IF NOT EXISTS payment_cutover (
  id INT PRIMARY KEY,
  state VARCHAR(40) NOT NULL,
  verified_at TIMESTAMP NULL
);
INSERT INTO payment_cutover(id,state) VALUES (1,'NOT_STARTED');
