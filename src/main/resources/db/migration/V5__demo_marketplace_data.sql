-- Demo accounts all use the password "demo123".
INSERT INTO tb_user (username, password) VALUES
('demo_bidder', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'),
('demo_seller', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'),
('demo_trader', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'),
('demo_newcomer', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq');

INSERT INTO tb_role (role, id_user) VALUES
('ROLE_USER', (SELECT id_user FROM tb_user WHERE username = 'demo_bidder')),
('ROLE_USER', (SELECT id_user FROM tb_user WHERE username = 'demo_seller')),
('ROLE_USER', (SELECT id_user FROM tb_user WHERE username = 'demo_trader')),
('ROLE_USER', (SELECT id_user FROM tb_user WHERE username = 'demo_newcomer'));

INSERT INTO tb_user_profile (first_name, last_name, phone_number, address, about, id_user) VALUES
('Mila', 'Bidic', '+381641110001', 'Belgrade, Serbia',
 'Buyer-only demo account with active, cancelled, denied, accepted, and paid bid history.',
 (SELECT id_user FROM tb_user WHERE username = 'demo_bidder')),
('Stefan', 'Prodic', '+381641110002', 'Novi Sad, Serbia',
 'Seller-only demo account with live, pending, inactive, reserved, and sold listings.',
 (SELECT id_user FROM tb_user WHERE username = 'demo_seller')),
('Ana', 'Trgovic', '+381641110003', 'Subotica, Serbia',
 'Marketplace trader with history as both a buyer and a seller.',
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader')),
('Luka', 'Novi', '+381641110004', 'Sombor, Serbia',
 'New marketplace member with no auction history yet.',
 (SELECT id_user FROM tb_user WHERE username = 'demo_newcomer'));

-- Seller-only inventory covers every listing lifecycle used by the UI.
INSERT INTO tb_car (make, model, production_year, status, price, id_user) VALUES
('Toyota', 'RAV4', '2021', 'ACTIVE', 24500,
 (SELECT id_user FROM tb_user WHERE username = 'demo_seller')),
('Honda', 'Civic', '2019', 'ACTIVE', 16800,
 (SELECT id_user FROM tb_user WHERE username = 'demo_seller')),
('Volvo', 'XC60', '2020', 'PENDING', 29800,
 (SELECT id_user FROM tb_user WHERE username = 'demo_seller')),
('Ford', 'Focus', '2018', 'DEACTIVE', 9800,
 (SELECT id_user FROM tb_user WHERE username = 'demo_seller')),
('Skoda', 'Octavia', '2020', 'SOLD', 17500,
 (SELECT id_user FROM tb_user WHERE username = 'demo_seller')),
('Mercedes-Benz', 'C200', '2021', 'RESERVED', 28900,
 (SELECT id_user FROM tb_user WHERE username = 'demo_seller'));

-- The trader owns listings and also bids on other sellers' cars.
INSERT INTO tb_car (make, model, production_year, status, price, id_user) VALUES
('BMW', '330i', '2022', 'ACTIVE', 32500,
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader')),
('Mazda', 'MX-5', '2017', 'SOLD', 18500,
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader')),
('Tesla', 'Model 3', '2023', 'PENDING', 35900,
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader')),
('Volkswagen', 'Golf', '2020', 'ACTIVE', 19900,
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader'));

-- A lightweight built-in SVG keeps every demo listing visually complete.
INSERT INTO tb_car_picture (file_name, file_type, image, id_car)
SELECT
  CONCAT('demo-car-', c.id_car, '.svg'),
  'image/svg+xml',
  'PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMjAwIiBoZWlnaHQ9IjY3NSIgdmlld0JveD0iMCAwIDEyMDAgNjc1Ij48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImciIHgxPSIwIiB5MT0iMCIgeDI9IjEiIHkyPSIxIj48c3RvcCBzdG9wLWNvbG9yPSIjMTAyNDNlIi8+PHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjMmY2ZjhmIi8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3Qgd2lkdGg9IjEyMDAiIGhlaWdodD0iNjc1IiBmaWxsPSJ1cmwoI2cpIi8+PGNpcmNsZSBjeD0iMzE1IiBjeT0iNDkyIiByPSI3MCIgZmlsbD0iIzEwMTgyMCIgc3Ryb2tlPSIjZjhiNDAwIiBzdHJva2Utd2lkdGg9IjE4Ii8+PGNpcmNsZSBjeD0iODg1IiBjeT0iNDkyIiByPSI3MCIgZmlsbD0iIzEwMTgyMCIgc3Ryb2tlPSIjZjhiNDAwIiBzdHJva2Utd2lkdGg9IjE4Ii8+PHBhdGggZD0iTTE4MCA0NjVoNDBsMTA1LTE0NWMyNS0zNSA2MS01NSAxMDQtNTVoMzEyYzQ4IDAgOTIgMjMgMTIwIDYybDkzIDEyOGg2NmMyOCAwIDUwIDIyIDUwIDUwdjE4SDEzMHYtOGMwLTI4IDIyLTUwIDUwLTUweiIgZmlsbD0iI2Y0ZjdmYSIvPjxwYXRoIGQ9Ik0zODQgMzIxaDE4MHYxMzRIMjg2bDgwLTExMGM1LTggMTEtMTYgMTgtMjR6bTIzMCAwaDExOWMyNCAwIDQ3IDEyIDYxIDMybDc1IDEwMkg2MTRWMzIxeiIgZmlsbD0iIzc5YjdkNCIvPjx0ZXh0IHg9IjYwMCIgeT0iMTI1IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0iQXJpYWwsc2Fucy1zZXJpZiIgZm9udC1zaXplPSI1OCIgZm9udC13ZWlnaHQ9IjcwMCI+U1VCT1RJTiBNT1RPUlM8L3RleHQ+PHRleHQgeD0iNjAwIiB5PSIxODUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZpbGw9IiNmOGI0MDAiIGZvbnQtZmFtaWx5PSJBcmlhbCxzYW5zLXNlcmlmIiBmb250LXNpemU9IjMwIj5ERU1PIFZFSElDTEU8L3RleHQ+PC9zdmc+',
  c.id_car
FROM tb_car c
JOIN tb_user u ON u.id_user = c.id_user
WHERE u.username IN ('demo_seller', 'demo_trader');

-- Demo payout accounts allow an administrator to accept the ongoing bids.
INSERT INTO tb_payment_account
  (id_user, provider_account_id, status, transfers_enabled, created_at, updated_at)
VALUES
((SELECT id_user FROM tb_user WHERE username = 'demo_seller'),
 'acct_demo_seller', 'ACTIVE', TRUE, '2026-05-01 09:00:00', '2026-05-01 09:00:00'),
((SELECT id_user FROM tb_user WHERE username = 'demo_trader'),
 'acct_demo_trader', 'ACTIVE', TRUE, '2026-05-02 09:00:00', '2026-05-02 09:00:00');

-- Current auctions and retained bid history.
INSERT INTO tb_car_bid (bid_price, status, id_user, id_car) VALUES
(25500, 'DENIED',
 (SELECT id_user FROM tb_user WHERE username = 'demo_bidder'),
 (SELECT id_car FROM tb_car WHERE make = 'Toyota' AND model = 'RAV4'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller'))),
(26200, 'ONGOING',
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader'),
 (SELECT id_car FROM tb_car WHERE make = 'Toyota' AND model = 'RAV4'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller'))),
(17400, 'CANCELLED',
 (SELECT id_user FROM tb_user WHERE username = 'demo_bidder'),
 (SELECT id_car FROM tb_car WHERE make = 'Honda' AND model = 'Civic'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller'))),
(17800, 'ONGOING',
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader'),
 (SELECT id_car FROM tb_car WHERE make = 'Honda' AND model = 'Civic'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller'))),
(33500, 'ONGOING',
 (SELECT id_user FROM tb_user WHERE username = 'demo_bidder'),
 (SELECT id_car FROM tb_car WHERE make = 'BMW' AND model = '330i'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_trader'))),
(20750, 'ONGOING',
 (SELECT id_user FROM tb_user WHERE username = 'demo_bidder'),
 (SELECT id_car FROM tb_car WHERE make = 'Volkswagen' AND model = 'Golf'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_trader'))),
(19200, 'PAID',
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader'),
 (SELECT id_car FROM tb_car WHERE make = 'Skoda' AND model = 'Octavia'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller'))),
(20500, 'PAID',
 (SELECT id_user FROM tb_user WHERE username = 'demo_bidder'),
 (SELECT id_car FROM tb_car WHERE make = 'Mazda' AND model = 'MX-5'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_trader'))),
(29800, 'DENIED',
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader'),
 (SELECT id_car FROM tb_car WHERE make = 'Mercedes-Benz' AND model = 'C200'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller'))),
(30200, 'ACCEPTED_PENDING_PAYMENT',
 (SELECT id_user FROM tb_user WHERE username = 'demo_bidder'),
 (SELECT id_car FROM tb_car WHERE make = 'Mercedes-Benz' AND model = 'C200'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller')));

INSERT INTO tb_payment_order
  (id_bid, id_buyer, id_seller, amount_minor, platform_fee_minor, currency, status,
   checkout_session_id, checkout_url, payment_intent_id, created_at, updated_at, paid_at)
VALUES
((SELECT id_bid FROM tb_car_bid
   WHERE id_car = (SELECT id_car FROM tb_car WHERE make = 'Skoda' AND model = 'Octavia'
     AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller'))
     AND status = 'PAID'),
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader'),
 (SELECT id_user FROM tb_user WHERE username = 'demo_seller'),
 1920000, 48000, 'eur', 'PAID',
 'cs_demo_skoda_paid', NULL, 'pi_demo_skoda_paid',
 '2026-05-10 12:00:00', '2026-05-10 12:05:00', '2026-05-10 12:05:00'),
((SELECT id_bid FROM tb_car_bid
   WHERE id_car = (SELECT id_car FROM tb_car WHERE make = 'Mazda' AND model = 'MX-5'
     AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_trader'))
     AND status = 'PAID'),
 (SELECT id_user FROM tb_user WHERE username = 'demo_bidder'),
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader'),
 2050000, 51250, 'eur', 'PAID',
 'cs_demo_mazda_paid', NULL, 'pi_demo_mazda_paid',
 '2026-05-18 14:00:00', '2026-05-18 14:04:00', '2026-05-18 14:04:00'),
((SELECT id_bid FROM tb_car_bid
   WHERE id_car = (SELECT id_car FROM tb_car WHERE make = 'Mercedes-Benz' AND model = 'C200'
     AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller'))
     AND status = 'ACCEPTED_PENDING_PAYMENT'),
 (SELECT id_user FROM tb_user WHERE username = 'demo_bidder'),
 (SELECT id_user FROM tb_user WHERE username = 'demo_seller'),
 3020000, 75500, 'eur', 'PENDING_CHECKOUT',
 NULL, NULL, NULL,
 '2026-06-19 08:30:00', '2026-06-19 08:30:00', NULL);

INSERT INTO tb_payment_webhook_event (provider_event_id, event_type, processed_at) VALUES
('evt_demo_skoda_paid', 'checkout.session.completed', '2026-05-10 12:05:00'),
('evt_demo_mazda_paid', 'checkout.session.completed', '2026-05-18 14:04:00');

-- Future dates keep these appointments usable in the demo for years.
INSERT INTO tb_test_drive (test_drive_date, status, id_user, id_car) VALUES
('2030-07-15', 'PENDING',
 (SELECT id_user FROM tb_user WHERE username = 'demo_bidder'),
 (SELECT id_car FROM tb_car WHERE make = 'Toyota' AND model = 'RAV4'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller'))),
('2030-07-20', 'ACCEPTED',
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader'),
 (SELECT id_car FROM tb_car WHERE make = 'Honda' AND model = 'Civic'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller'))),
('2030-07-25', 'REJECTED',
 (SELECT id_user FROM tb_user WHERE username = 'demo_bidder'),
 (SELECT id_car FROM tb_car WHERE make = 'BMW' AND model = '330i'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_trader'))),
('2030-08-01', 'CANCELLED',
 (SELECT id_user FROM tb_user WHERE username = 'demo_trader'),
 (SELECT id_car FROM tb_car WHERE make = 'Toyota' AND model = 'RAV4'
   AND id_user = (SELECT id_user FROM tb_user WHERE username = 'demo_seller')));
