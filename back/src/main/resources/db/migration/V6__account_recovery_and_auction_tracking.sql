ALTER TABLE tb_user
  ADD COLUMN email VARCHAR(254) NOT NULL DEFAULT 'pending@subotinmotors.local';

UPDATE tb_user
SET email = CONCAT(LOWER(username), '@subotinmotors.local');

ALTER TABLE tb_user
  ADD CONSTRAINT uk_user_email UNIQUE (email);

ALTER TABLE tb_car
  ADD COLUMN auction_end_time TIMESTAMP NOT NULL DEFAULT '2030-12-31 23:59:59';

UPDATE tb_car
SET auction_end_time = CASE
  WHEN status = 'SOLD' THEN TIMESTAMPADD(DAY, -30, CURRENT_TIMESTAMP)
  WHEN status = 'RESERVED' THEN TIMESTAMPADD(HOUR, 12, CURRENT_TIMESTAMP)
  WHEN status = 'DEACTIVE' THEN TIMESTAMPADD(DAY, 14, CURRENT_TIMESTAMP)
  WHEN status = 'PENDING' THEN TIMESTAMPADD(DAY, 21, CURRENT_TIMESTAMP)
  ELSE TIMESTAMPADD(DAY, 7, CURRENT_TIMESTAMP)
END;

UPDATE tb_car
SET auction_end_time = TIMESTAMPADD(HOUR, 2, CURRENT_TIMESTAMP)
WHERE make = 'Toyota' AND model = 'RAV4';

UPDATE tb_car
SET auction_end_time = TIMESTAMPADD(DAY, 2, CURRENT_TIMESTAMP)
WHERE make = 'Honda' AND model = 'Civic';

UPDATE tb_car
SET auction_end_time = TIMESTAMPADD(HOUR, 8, CURRENT_TIMESTAMP)
WHERE make = 'BMW' AND model = '330i';

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

CREATE TABLE tb_auction_follow (
    id_follow INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT NOT NULL,
    id_car INT NOT NULL,
    followed_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_auction_follow_user_car UNIQUE (id_user, id_car),
    CONSTRAINT fk_auction_follow_user FOREIGN KEY (id_user) REFERENCES tb_user(id_user),
    CONSTRAINT fk_auction_follow_car FOREIGN KEY (id_car) REFERENCES tb_car(id_car)
);

CREATE TABLE tb_auction_notification (
    id_notification INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT NOT NULL,
    id_car INT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    read_at TIMESTAMP,
    CONSTRAINT uk_notification_user_car_type UNIQUE (id_user, id_car, notification_type),
    CONSTRAINT fk_notification_user FOREIGN KEY (id_user) REFERENCES tb_user(id_user),
    CONSTRAINT fk_notification_car FOREIGN KEY (id_car) REFERENCES tb_car(id_car)
);

INSERT INTO tb_auction_follow (id_user, id_car, followed_at)
SELECT
  (SELECT id_user FROM tb_user WHERE username = 'demo_newcomer'),
  id_car,
  CURRENT_TIMESTAMP
FROM tb_car
WHERE make IN ('Toyota', 'BMW') AND status = 'ACTIVE';

INSERT INTO tb_auction_notification
  (id_user, id_car, notification_type, message, created_at, read_at)
SELECT
  (SELECT id_user FROM tb_user WHERE username = 'demo_newcomer'),
  id_car,
  'AUCTION_ENDING_SOON',
  CONCAT(make, ' ', model, ' is ending soon.'),
  CURRENT_TIMESTAMP,
  NULL
FROM tb_car
WHERE make = 'Toyota' AND model = 'RAV4';
