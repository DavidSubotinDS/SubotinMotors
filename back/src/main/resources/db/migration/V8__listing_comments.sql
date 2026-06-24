CREATE TABLE tb_listing_comment (
    id_comment INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT NOT NULL,
    id_car INT,
    id_part INT,
    body VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_listing_comment_user
      FOREIGN KEY (id_user) REFERENCES tb_user(id_user),
    CONSTRAINT fk_listing_comment_car
      FOREIGN KEY (id_car) REFERENCES tb_car(id_car),
    CONSTRAINT fk_listing_comment_part
      FOREIGN KEY (id_part) REFERENCES tb_car_part(id_part),
    CONSTRAINT ck_listing_comment_single_target CHECK (
      (id_car IS NOT NULL AND id_part IS NULL)
      OR (id_car IS NULL AND id_part IS NOT NULL)
    )
);

CREATE INDEX idx_listing_comment_car
  ON tb_listing_comment(id_car, created_at, id_comment);

CREATE INDEX idx_listing_comment_part
  ON tb_listing_comment(id_part, created_at, id_comment);

-- Auction discussion: buyer question, seller answer, and an administrative reminder.
INSERT INTO tb_listing_comment (id_user, id_car, body, created_at) VALUES
((SELECT id_user FROM tb_user WHERE username = 'demo_bidder'),
 (SELECT id_car FROM tb_car
   WHERE make = 'Toyota' AND model = 'RAV4'
     AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller')),
 'Does the RAV4 come with both sets of tires, and is the service history available?',
 '2026-06-18 09:10:00'),
((SELECT id_user FROM tb_user WHERE username = 'demo_seller'),
 (SELECT id_car FROM tb_car
   WHERE make = 'Toyota' AND model = 'RAV4'
     AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller')),
 'Yes, summer and winter tires are included. I can show the complete digital service history during the test drive.',
 '2026-06-18 09:42:00'),
((SELECT id_user FROM tb_user WHERE username = 'admin123'),
 (SELECT id_car FROM tb_car
   WHERE make = 'Toyota' AND model = 'RAV4'
     AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller')),
 'Friendly reminder: keep payment and ownership-transfer arrangements inside the marketplace workflow.',
 '2026-06-18 10:05:00');

INSERT INTO tb_listing_comment (id_user, id_car, body, created_at) VALUES
((SELECT id_user FROM tb_user WHERE username = 'demo_newcomer'),
 (SELECT id_car FROM tb_car
   WHERE make = 'BMW' AND model = '330i'
     AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_trader')),
 'Has the automatic transmission oil been changed, and are there any warning lights?',
 '2026-06-19 14:20:00'),
((SELECT id_user FROM tb_user WHERE username = 'demo_trader'),
 (SELECT id_car FROM tb_car
   WHERE make = 'BMW' AND model = '330i'
     AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_trader')),
 'The transmission service was completed last year and there are no warning lights. I have the invoice.',
 '2026-06-19 15:02:00');

-- Parts-store discussion: customer questions and clearly identified store-team replies.
INSERT INTO tb_listing_comment (id_user, id_part, body, created_at) VALUES
((SELECT id_user FROM tb_user WHERE username = 'demo_bidder'),
 (SELECT id_part FROM tb_car_part WHERE sku = 'BRK-PAD-001'),
 'Will these fit a 2019 Honda Civic hatchback with the standard front brakes?',
 '2026-06-20 08:30:00'),
((SELECT id_user FROM tb_user WHERE username = 'admin123'),
 (SELECT id_part FROM tb_car_part WHERE sku = 'BRK-PAD-001'),
 'They fit several Civic configurations, but please compare the pad dimensions with your VIN specification before ordering.',
 '2026-06-20 09:00:00'),
((SELECT id_user FROM tb_user WHERE username = 'demo_trader'),
 (SELECT id_part FROM tb_car_part WHERE sku = 'BAT-AGM-070'),
 'Is this battery delivered charged and ready to install?',
 '2026-06-20 11:15:00'),
((SELECT id_user FROM tb_user WHERE username = 'admin123'),
 (SELECT id_part FROM tb_car_part WHERE sku = 'BAT-AGM-070'),
 'Yes. It is dispatched fully charged, although some vehicles will need battery registration after installation.',
 '2026-06-20 11:40:00'),
((SELECT id_user FROM tb_user WHERE username = 'demo_newcomer'),
 (SELECT id_part FROM tb_car_part WHERE sku = 'OIL-5W30-5L'),
 'The specification details were useful and the container arrived well protected.',
 '2026-06-21 16:25:00');
