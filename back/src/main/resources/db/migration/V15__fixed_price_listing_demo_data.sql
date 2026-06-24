-- Additional fixed-price marketplace demo accounts all use the password "demo123".
INSERT INTO tb_user (username, email, password)
SELECT demo.username, CONCAT(demo.username, '@subotinmotors.local'), demo.password
FROM (
  SELECT 'demo_list_01' AS username, '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq' AS password
  UNION ALL SELECT 'demo_list_02', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_03', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_04', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_05', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_06', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_07', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_08', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_09', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_10', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_11', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_12', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_13', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_14', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_15', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_16', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_17', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_18', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_19', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_20', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_21', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_22', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_23', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
  UNION ALL SELECT 'demo_list_24', '$2a$10$Ger96676quQzhqUTT7D4.OQL9XXgjTXoGj.dOnAIh6SPZAzvE9POq'
) demo
WHERE NOT EXISTS (
  SELECT 1 FROM tb_user existing_user WHERE existing_user.username = demo.username
);

INSERT INTO tb_role (role, id_user)
SELECT 'ROLE_USER', u.id_user
FROM tb_user u
WHERE u.username LIKE 'demo!_list!_%' ESCAPE '!'
  AND NOT EXISTS (
    SELECT 1 FROM tb_role r
    WHERE r.id_user = u.id_user
      AND r.role = 'ROLE_USER'
  );

INSERT INTO tb_user_profile
  (first_name, last_name, phone_number, address, about, id_user,
   street_address, city, postal_code, country)
SELECT
  demo.first_name,
  demo.last_name,
  demo.phone_number,
  demo.city_country,
  demo.about,
  u.id_user,
  demo.street_address,
  demo.city,
  demo.postal_code,
  demo.country
FROM (
  SELECT 'Marko' AS first_name, 'Kovacevic' AS last_name, '+381642200001' AS phone_number, 'Belgrade, Serbia' AS city_country,
    'Fixed-price seller demo account for a practical compact sedan listing.' AS about,
    'Bulevar kralja Aleksandra 108' AS street_address, 'Belgrade' AS city, '11000' AS postal_code, 'Serbia' AS country,
    'demo_list_01' AS username
  UNION ALL SELECT 'Jelena', 'Petrovic', '+381642200002', 'Novi Sad, Serbia',
    'Fixed-price seller demo account for a city hatchback listing.',
    'Zmaj Jovina 12', 'Novi Sad', '21000', 'Serbia', 'demo_list_02'
  UNION ALL SELECT 'Nikola', 'Ilic', '+381642200003', 'Subotica, Serbia',
    'Fixed-price seller demo account for a family diesel estate listing.',
    'Korzo 6', 'Subotica', '24000', 'Serbia', 'demo_list_03'
  UNION ALL SELECT 'Sara', 'Jovanovic', '+381642200004', 'Kragujevac, Serbia',
    'Fixed-price seller demo account with a reserved electric sedan.',
    'Kralja Petra I 24', 'Kragujevac', '34000', 'Serbia', 'demo_list_04'
  UNION ALL SELECT 'Viktor', 'Stankovic', '+381642200005', 'Nis, Serbia',
    'Fixed-price seller demo account for a premium compact SUV listing.',
    'Bulevar Nemanjica 77', 'Nis', '18000', 'Serbia', 'demo_list_05'
  UNION ALL SELECT 'Milica', 'Savic', '+381642200006', 'Zrenjanin, Serbia',
    'Fixed-price seller demo account for a tidy wagon listing.',
    'Kralja Aleksandra I 15', 'Zrenjanin', '23000', 'Serbia', 'demo_list_06'
  UNION ALL SELECT 'Dusan', 'Markovic', '+381642200007', 'Cacak, Serbia',
    'Fixed-price seller demo account with a completed sale example.',
    'Gospodar Jovanova 4', 'Cacak', '32000', 'Serbia', 'demo_list_07'
  UNION ALL SELECT 'Tamara', 'Popovic', '+381642200008', 'Sombor, Serbia',
    'Fixed-price seller demo account for an economical first-car listing.',
    'Venac Radomira Putnika 18', 'Sombor', '25000', 'Serbia', 'demo_list_08'
  UNION ALL SELECT 'Lazar', 'Matic', '+381642200009', 'Pancevo, Serbia',
    'Fixed-price seller demo account for a modern crossover listing.',
    'Vojvode Radomira Putnika 31', 'Pancevo', '26000', 'Serbia', 'demo_list_09'
  UNION ALL SELECT 'Ivana', 'Ristic', '+381642200010', 'Kraljevo, Serbia',
    'Fixed-price seller demo account for a family SUV listing.',
    'Omladinska 9', 'Kraljevo', '36000', 'Serbia', 'demo_list_10'
  UNION ALL SELECT 'Filip', 'Pavlovic', '+381642200011', 'Uzice, Serbia',
    'Fixed-price seller demo account with an inactive draft listing.',
    'Dimitrija Tucovica 55', 'Uzice', '31000', 'Serbia', 'demo_list_11'
  UNION ALL SELECT 'Teodora', 'Milosevic', '+381642200012', 'Valjevo, Serbia',
    'Fixed-price seller demo account with a reserved adventure SUV.',
    'Knez Mihailova 28', 'Valjevo', '14000', 'Serbia', 'demo_list_12'
  UNION ALL SELECT 'Ognjen', 'Lazic', '+381642200013', 'Leskovac, Serbia',
    'Fixed-price seller demo account for a well-kept French crossover.',
    'Bulevar oslobodjenja 44', 'Leskovac', '16000', 'Serbia', 'demo_list_13'
  UNION ALL SELECT 'Mina', 'Djordjevic', '+381642200014', 'Vrsac, Serbia',
    'Fixed-price seller demo account for a compact family crossover.',
    'Svetosavski trg 3', 'Vrsac', '26300', 'Serbia', 'demo_list_14'
  UNION ALL SELECT 'Petar', 'Vasic', '+381642200015', 'Sremska Mitrovica, Serbia',
    'Fixed-price seller demo account for a stylish city car.',
    'Kralja Petra I 41', 'Sremska Mitrovica', '22000', 'Serbia', 'demo_list_15'
  UNION ALL SELECT 'Anja', 'Nikolic', '+381642200016', 'Sabac, Serbia',
    'Fixed-price seller demo account for a comfortable executive wagon.',
    'Masarikova 20', 'Sabac', '15000', 'Serbia', 'demo_list_16'
  UNION ALL SELECT 'Bogdan', 'Zivkovic', '+381642200017', 'Kikinda, Serbia',
    'Fixed-price seller demo account with a sold performance SUV.',
    'Trg srpskih dobrovoljaca 11', 'Kikinda', '23300', 'Serbia', 'demo_list_17'
  UNION ALL SELECT 'Una', 'Stefanovic', '+381642200018', 'Pozarevac, Serbia',
    'Fixed-price seller demo account for a reliable hatchback.',
    'Moše Pijade 7', 'Pozarevac', '12000', 'Serbia', 'demo_list_18'
  UNION ALL SELECT 'Andrej', 'Cvetkovic', '+381642200019', 'Loznica, Serbia',
    'Fixed-price seller demo account for a rugged budget SUV.',
    'Jovana Cvijica 36', 'Loznica', '15300', 'Serbia', 'demo_list_19'
  UNION ALL SELECT 'Nina', 'Bogdanovic', '+381642200020', 'Jagodina, Serbia',
    'Fixed-price seller demo account with a reserved premium hybrid.',
    'Kneginje Milice 13', 'Jagodina', '35000', 'Serbia', 'demo_list_20'
  UNION ALL SELECT 'Relja', 'Radovic', '+381642200021', 'Pirot, Serbia',
    'Fixed-price seller demo account for a cheerful compact hatchback.',
    'Srpskih vladara 22', 'Pirot', '18300', 'Serbia', 'demo_list_21'
  UNION ALL SELECT 'Lena', 'Tomic', '+381642200022', 'Bor, Serbia',
    'Fixed-price seller demo account for a sharp compact estate.',
    'Moše Pijade 61', 'Bor', '19210', 'Serbia', 'demo_list_22'
  UNION ALL SELECT 'Mihajlo', 'Peric', '+381642200023', 'Kovin, Serbia',
    'Fixed-price seller demo account for an enthusiast sport sedan.',
    'Cara Lazara 17', 'Kovin', '26220', 'Serbia', 'demo_list_23'
  UNION ALL SELECT 'Isidora', 'Blagojevic', '+381642200024', 'Ruma, Serbia',
    'Fixed-price seller demo account for a nearly new electric crossover.',
    'Glavna 84', 'Ruma', '22400', 'Serbia', 'demo_list_24'
) demo
JOIN tb_user u ON u.username = demo.username
WHERE NOT EXISTS (
  SELECT 1 FROM tb_user_profile p WHERE p.id_user = u.id_user
);

INSERT INTO tb_car_listing
  (title, make, model, production_year, mileage, fuel_type, transmission,
   price_minor, deposit_amount_minor, description, status, id_seller, created_at, updated_at)
SELECT
  demo.title,
  demo.make,
  demo.model,
  demo.production_year,
  demo.mileage,
  demo.fuel_type,
  demo.transmission,
  demo.price_minor,
  demo.deposit_amount_minor,
  demo.description,
  demo.status,
  u.id_user,
  demo.created_at,
  demo.updated_at
FROM (
  SELECT 'Toyota Corolla Hybrid Comfort' AS title, 'Toyota' AS make, 'Corolla' AS model, '2021' AS production_year,
    46500 AS mileage, 'Hybrid' AS fuel_type, 'Automatic' AS transmission, 1890000 AS price_minor, 95000 AS deposit_amount_minor,
    'One-owner compact sedan with documented service history, winter tires, parking sensors, and very low running costs.' AS description,
    'ACTIVE' AS status, 'demo_list_01' AS username, '2026-06-01 09:00:00' AS created_at, '2026-06-01 09:00:00' AS updated_at
  UNION ALL SELECT 'Honda Jazz 1.5 i-MMD Elegance', 'Honda', 'Jazz', '2020',
    38200, 'Hybrid', 'Automatic', 1590000, 80000,
    'Flexible city hatchback with Magic Seats, adaptive cruise control, fresh service, and excellent visibility.',
    'ACTIVE', 'demo_list_02', '2026-06-01 10:00:00', '2026-06-01 10:00:00'
  UNION ALL SELECT 'Volkswagen Passat Variant TDI', 'Volkswagen', 'Passat', '2019',
    90500, 'Diesel', 'Automatic', 1745000, 90000,
    'Spacious estate with DSG gearbox, highway mileage, roof rails, and a clean family-use interior.',
    'ACTIVE', 'demo_list_03', '2026-06-01 11:00:00', '2026-06-01 11:00:00'
  UNION ALL SELECT 'Mercedes-Benz EQE 350+', 'Mercedes-Benz', 'EQE', '2023',
    14800, 'Electric', 'Automatic', 6290000, 250000,
    'Reserved demo electric sedan with premium cabin, long-range battery, panoramic roof, and full dealer maintenance.',
    'RESERVED', 'demo_list_04', '2026-06-01 12:00:00', '2026-06-01 12:00:00'
  UNION ALL SELECT 'BMW X1 xDrive20d Advantage', 'BMW', 'X1', '2020',
    68800, 'Diesel', 'Automatic', 2790000, 140000,
    'Compact premium SUV with all-wheel drive, navigation, heated seats, and a recent inspection.',
    'ACTIVE', 'demo_list_05', '2026-06-02 09:00:00', '2026-06-02 09:00:00'
  UNION ALL SELECT 'Audi A4 Avant 35 TFSI', 'Audi', 'A4 Avant', '2021',
    52200, 'Petrol', 'Automatic', 3090000, 150000,
    'Elegant wagon with virtual cockpit, LED headlights, two keys, and a carefully maintained interior.',
    'ACTIVE', 'demo_list_06', '2026-06-02 10:00:00', '2026-06-02 10:00:00'
  UNION ALL SELECT 'Skoda Superb 2.0 TDI Style', 'Skoda', 'Superb', '2018',
    118000, 'Diesel', 'Automatic', 1490000, 75000,
    'Sold demo listing that remains available to show seller history and completed fixed-price stock.',
    'SOLD', 'demo_list_07', '2026-06-02 11:00:00', '2026-06-02 11:00:00'
  UNION ALL SELECT 'Renault Clio TCe Intens', 'Renault', 'Clio', '2021',
    41300, 'Petrol', 'Manual', 1190000, 60000,
    'Efficient hatchback with touchscreen media, lane assist, alloy wheels, and affordable insurance.',
    'ACTIVE', 'demo_list_08', '2026-06-02 12:00:00', '2026-06-02 12:00:00'
  UNION ALL SELECT 'Hyundai Tucson 1.6 T-GDi Hybrid', 'Hyundai', 'Tucson', '2022',
    29700, 'Hybrid', 'Automatic', 3190000, 160000,
    'Modern family crossover with safety pack, camera, heated steering wheel, and remaining factory warranty.',
    'ACTIVE', 'demo_list_09', '2026-06-03 09:00:00', '2026-06-03 09:00:00'
  UNION ALL SELECT 'Kia Sportage 1.6 CRDi EX', 'Kia', 'Sportage', '2020',
    74400, 'Diesel', 'Manual', 1990000, 100000,
    'Well-kept SUV with service book, Android Auto, reverse camera, and a comfortable family setup.',
    'ACTIVE', 'demo_list_10', '2026-06-03 10:00:00', '2026-06-03 10:00:00'
  UNION ALL SELECT 'Ford Puma EcoBoost Titanium', 'Ford', 'Puma', '2021',
    33800, 'Petrol', 'Manual', 1690000, 85000,
    'Inactive draft listing kept in demo data so owner dashboards show non-public inventory.',
    'INACTIVE', 'demo_list_11', '2026-06-03 11:00:00', '2026-06-03 11:00:00'
  UNION ALL SELECT 'Land Rover Defender 110 D250', 'Land Rover', 'Defender', '2022',
    35400, 'Diesel', 'Automatic', 7190000, 300000,
    'Reserved adventure SUV with seven seats, air suspension, tow package, and all-terrain tires.',
    'RESERVED', 'demo_list_12', '2026-06-03 12:00:00', '2026-06-03 12:00:00'
  UNION ALL SELECT 'Peugeot 3008 PureTech Allure', 'Peugeot', '3008', '2020',
    57200, 'Petrol', 'Automatic', 2140000, 110000,
    'Comfortable crossover with i-Cockpit, parking camera, clean cabin, and a smooth automatic gearbox.',
    'ACTIVE', 'demo_list_13', '2026-06-04 09:00:00', '2026-06-04 09:00:00'
  UNION ALL SELECT 'Nissan Qashqai DIG-T N-Connecta', 'Nissan', 'Qashqai', '2019',
    69500, 'Petrol', 'Manual', 1640000, 85000,
    'Popular crossover with panoramic roof, navigation, camera, and recent brake service.',
    'ACTIVE', 'demo_list_14', '2026-06-04 10:00:00', '2026-06-04 10:00:00'
  UNION ALL SELECT 'Fiat 500 Dolcevita', 'Fiat', '500', '2021',
    25600, 'Petrol', 'Manual', 1090000, 55000,
    'Stylish city car with glass roof, low mileage, two-tone trim, and fresh summer tires.',
    'ACTIVE', 'demo_list_15', '2026-06-04 11:00:00', '2026-06-04 11:00:00'
  UNION ALL SELECT 'Volvo V90 B4 Momentum', 'Volvo', 'V90', '2021',
    63100, 'Diesel', 'Automatic', 3590000, 180000,
    'Executive estate with safety technology, leather interior, large cargo space, and serene highway comfort.',
    'ACTIVE', 'demo_list_16', '2026-06-04 12:00:00', '2026-06-04 12:00:00'
  UNION ALL SELECT 'Porsche Macan S', 'Porsche', 'Macan', '2019',
    84400, 'Petrol', 'Automatic', 5290000, 220000,
    'Sold performance SUV example with sport exhaust, PASM, Bose audio, and documented ownership.',
    'SOLD', 'demo_list_17', '2026-06-05 09:00:00', '2026-06-05 09:00:00'
  UNION ALL SELECT 'Opel Astra 1.2 Turbo Edition', 'Opel', 'Astra', '2020',
    58900, 'Petrol', 'Manual', 1290000, 65000,
    'Reliable compact hatchback with economical engine, Apple CarPlay, and a tidy service record.',
    'ACTIVE', 'demo_list_18', '2026-06-05 10:00:00', '2026-06-05 10:00:00'
  UNION ALL SELECT 'Dacia Duster Blue dCi Prestige', 'Dacia', 'Duster', '2019',
    78100, 'Diesel', 'Manual', 1390000, 70000,
    'Rugged value SUV with raised clearance, simple maintenance, parking sensors, and winter wheel set.',
    'ACTIVE', 'demo_list_19', '2026-06-05 11:00:00', '2026-06-05 11:00:00'
  UNION ALL SELECT 'Lexus NX 350h Executive', 'Lexus', 'NX', '2022',
    24100, 'Hybrid', 'Automatic', 4890000, 210000,
    'Reserved premium hybrid SUV with quiet cabin, warranty coverage, memory seats, and full service history.',
    'RESERVED', 'demo_list_20', '2026-06-05 12:00:00', '2026-06-05 12:00:00'
  UNION ALL SELECT 'Mini Cooper 1.5 Chili', 'Mini', 'Cooper', '2020',
    45300, 'Petrol', 'Automatic', 1790000, 90000,
    'Playful compact hatch with automatic transmission, LED lights, sport seats, and distinctive styling.',
    'ACTIVE', 'demo_list_21', '2026-06-06 09:00:00', '2026-06-06 09:00:00'
  UNION ALL SELECT 'Seat Leon Sportstourer FR', 'Seat', 'Leon', '2021',
    48600, 'Petrol', 'Automatic', 2090000, 105000,
    'Sharp estate with FR trim, digital cockpit, adaptive cruise, and practical load space.',
    'ACTIVE', 'demo_list_22', '2026-06-06 10:00:00', '2026-06-06 10:00:00'
  UNION ALL SELECT 'Alfa Romeo Giulia Veloce', 'Alfa Romeo', 'Giulia', '2020',
    60400, 'Petrol', 'Automatic', 3490000, 175000,
    'Driver-focused sport sedan with paddle shifters, leather trim, beautiful steering, and recent service.',
    'ACTIVE', 'demo_list_23', '2026-06-06 11:00:00', '2026-06-06 11:00:00'
  UNION ALL SELECT 'Tesla Model Y Long Range AWD', 'Tesla', 'Model Y', '2023',
    18700, 'Electric', 'Automatic', 4590000, 200000,
    'Nearly new electric crossover with dual motor AWD, heat pump, panoramic roof, and software updates.',
    'ACTIVE', 'demo_list_24', '2026-06-06 12:00:00', '2026-06-06 12:00:00'
) demo
JOIN tb_user u ON u.username = demo.username
WHERE NOT EXISTS (
  SELECT 1 FROM tb_car_listing l
  WHERE l.id_seller = u.id_user
    AND l.title = demo.title
);

INSERT INTO tb_car_listing_picture (file_name, file_type, image, id_listing)
SELECT
  CONCAT('demo-listing-', l.id_listing, '.svg'),
  'image/svg+xml',
  'PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmciIHdpZHRoPSIxMjAwIiBoZWlnaHQ9IjY3NSIgdmlld0JveD0iMCAwIDEyMDAgNjc1Ij48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImciIHgxPSIwIiB5MT0iMCIgeDI9IjEiIHkyPSIxIj48c3RvcCBzdG9wLWNvbG9yPSIjMTAyNDNlIi8+PHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjMmY2ZjhmIi8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3Qgd2lkdGg9IjEyMDAiIGhlaWdodD0iNjc1IiBmaWxsPSJ1cmwoI2cpIi8+PGNpcmNsZSBjeD0iMzE1IiBjeT0iNDkyIiByPSI3MCIgZmlsbD0iIzEwMTgyMCIgc3Ryb2tlPSIjZjhiNDAwIiBzdHJva2Utd2lkdGg9IjE4Ii8+PGNpcmNsZSBjeD0iODg1IiBjeT0iNDkyIiByPSI3MCIgZmlsbD0iIzEwMTgyMCIgc3Ryb2tlPSIjZjhiNDAwIiBzdHJva2Utd2lkdGg9IjE4Ii8+PHBhdGggZD0iTTE4MCA0NjVoNDBsMTA1LTE0NWMyNS0zNSA2MS01NSAxMDQtNTVoMzEyYzQ4IDAgOTIgMjMgMTIwIDYybDkzIDEyOGg2NmMyOCAwIDUwIDIyIDUwIDUwdjE4SDEzMHYtOGMwLTI4IDIyLTUwIDUwLTUweiIgZmlsbD0iI2Y0ZjdmYSIvPjxwYXRoIGQ9Ik0zODQgMzIxaDE4MHYxMzRIMjg2bDgwLTExMGM1LTggMTEtMTYgMTgtMjR6bTIzMCAwaDExOWMyNCAwIDQ3IDEyIDYxIDMybDc1IDEwMkg2MTRWMzIxeiIgZmlsbD0iIzc5YjdkNCIvPjx0ZXh0IHg9IjYwMCIgeT0iMTI1IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmaWxsPSIjZmZmZmZmIiBmb250LWZhbWlseT0iQXJpYWwsc2Fucy1zZXJpZiIgZm9udC1zaXplPSI1OCIgZm9udC13ZWlnaHQ9IjcwMCI+U1VCT1RJTiBNT1RPUlM8L3RleHQ+PHRleHQgeD0iNjAwIiB5PSIxODUiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZpbGw9IiNmOGI0MDAiIGZvbnQtZmFtaWx5PSJBcmlhbCxzYW5zLXNlcmlmIiBmb250LXNpemU9IjMwIj5ERU1PIFZFSElDTEU8L3RleHQ+PC9zdmc+',
  l.id_listing
FROM tb_car_listing l
JOIN tb_user u ON u.id_user = l.id_seller
WHERE u.username LIKE 'demo!_list!_%' ESCAPE '!'
  AND NOT EXISTS (
    SELECT 1 FROM tb_car_listing_picture p WHERE p.id_listing = l.id_listing
  );

INSERT INTO tb_listing_test_ride
  (scheduled_at, status, id_listing, id_user, created_at, updated_at)
SELECT
  demo.scheduled_at,
  demo.status,
  l.id_listing,
  rider.id_user,
  demo.created_at,
  demo.updated_at
FROM (
  SELECT '2031-07-10 10:00:00' AS scheduled_at, 'PENDING' AS status,
    'Toyota Corolla Hybrid Comfort' AS title, 'demo_list_01' AS seller_username,
    'demo_bidder' AS rider_username, '2026-06-10 10:00:00' AS created_at, '2026-06-10 10:00:00' AS updated_at
  UNION ALL SELECT '2031-07-11 14:30:00', 'ACCEPTED',
    'Honda Jazz 1.5 i-MMD Elegance', 'demo_list_02',
    'demo_trader', '2026-06-10 11:00:00', '2026-06-10 11:05:00'
  UNION ALL SELECT '2031-07-12 09:30:00', 'REJECTED',
    'BMW X1 xDrive20d Advantage', 'demo_list_05',
    'user123', '2026-06-10 12:00:00', '2026-06-10 12:10:00'
  UNION ALL SELECT '2031-07-13 16:00:00', 'CANCELLED',
    'Hyundai Tucson 1.6 T-GDi Hybrid', 'demo_list_09',
    'demo_newcomer', '2026-06-10 13:00:00', '2026-06-10 13:20:00'
  UNION ALL SELECT '2031-07-15 11:00:00', 'PENDING',
    'Tesla Model Y Long Range AWD', 'demo_list_24',
    'demo_bidder', '2026-06-10 14:00:00', '2026-06-10 14:00:00'
) demo
JOIN tb_user seller ON seller.username = demo.seller_username
JOIN tb_car_listing l ON l.id_seller = seller.id_user AND l.title = demo.title
JOIN tb_user rider ON rider.username = demo.rider_username
WHERE NOT EXISTS (
  SELECT 1 FROM tb_listing_test_ride existing_ride
  WHERE existing_ride.id_listing = l.id_listing
    AND existing_ride.id_user = rider.id_user
    AND existing_ride.scheduled_at = demo.scheduled_at
);

INSERT INTO tb_listing_deposit
  (id_listing, id_buyer, amount_minor, currency, status, checkout_session_id,
   checkout_url, payment_intent_id, created_at, updated_at, paid_at)
SELECT
  l.id_listing,
  buyer.id_user,
  demo.amount_minor,
  'eur',
  demo.status,
  demo.checkout_session_id,
  demo.checkout_url,
  demo.payment_intent_id,
  demo.created_at,
  demo.updated_at,
  demo.paid_at
FROM (
  SELECT 'Mercedes-Benz EQE 350+' AS title, 'demo_list_04' AS seller_username,
    'demo_bidder' AS buyer_username, 250000 AS amount_minor, 'PENDING_CHECKOUT' AS status,
    'cs_demo_listing_eqe_pending' AS checkout_session_id,
    'http://localhost:8080/demo/listing-deposits/eqe' AS checkout_url,
    NULL AS payment_intent_id, '2026-06-12 09:00:00' AS created_at,
    '2026-06-12 09:00:00' AS updated_at, NULL AS paid_at
  UNION ALL SELECT 'Land Rover Defender 110 D250', 'demo_list_12',
    'demo_trader', 300000, 'PAID',
    'cs_demo_listing_defender_paid',
    'http://localhost:8080/demo/listing-deposits/defender',
    'pi_demo_listing_defender_paid', '2026-06-12 10:00:00',
    '2026-06-12 10:04:00', '2026-06-12 10:04:00'
  UNION ALL SELECT 'Lexus NX 350h Executive', 'demo_list_20',
    'user123', 210000, 'PAID',
    'cs_demo_listing_lexus_paid',
    'http://localhost:8080/demo/listing-deposits/lexus',
    'pi_demo_listing_lexus_paid', '2026-06-12 11:00:00',
    '2026-06-12 11:03:00', '2026-06-12 11:03:00'
  UNION ALL SELECT 'Audi A4 Avant 35 TFSI', 'demo_list_06',
    'demo_bidder', 150000, 'EXPIRED',
    'cs_demo_listing_audi_expired',
    'http://localhost:8080/demo/listing-deposits/audi',
    NULL, '2026-06-12 12:00:00',
    '2026-06-12 12:30:00', NULL
) demo
JOIN tb_user seller ON seller.username = demo.seller_username
JOIN tb_car_listing l ON l.id_seller = seller.id_user AND l.title = demo.title
JOIN tb_user buyer ON buyer.username = demo.buyer_username
WHERE NOT EXISTS (
  SELECT 1 FROM tb_listing_deposit existing_deposit
  WHERE existing_deposit.checkout_session_id = demo.checkout_session_id
);
