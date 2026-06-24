CREATE TABLE tb_car_part (
    id_part INT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(80) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    price_minor BIGINT NOT NULL,
    stock_quantity INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_car_part_sku UNIQUE (sku),
    CONSTRAINT ck_car_part_price CHECK (price_minor > 0),
    CONSTRAINT ck_car_part_stock CHECK (stock_quantity >= 0)
);

CREATE TABLE tb_cart_item (
    id_cart_item INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT NOT NULL,
    id_part INT NOT NULL,
    quantity INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_cart_user_part UNIQUE (id_user, id_part),
    CONSTRAINT ck_cart_item_quantity CHECK (quantity > 0),
    CONSTRAINT fk_cart_item_user FOREIGN KEY (id_user) REFERENCES tb_user(id_user),
    CONSTRAINT fk_cart_item_part FOREIGN KEY (id_part) REFERENCES tb_car_part(id_part)
);

CREATE TABLE tb_store_order (
    id_order INT AUTO_INCREMENT PRIMARY KEY,
    id_user INT NOT NULL,
    total_minor BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    shipping_name VARCHAR(120) NOT NULL,
    shipping_address VARCHAR(500) NOT NULL,
    checkout_session_id VARCHAR(255),
    checkout_url VARCHAR(2000),
    payment_intent_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_store_order_checkout UNIQUE (checkout_session_id),
    CONSTRAINT uk_store_order_intent UNIQUE (payment_intent_id),
    CONSTRAINT fk_store_order_user FOREIGN KEY (id_user) REFERENCES tb_user(id_user)
);

CREATE TABLE tb_store_order_item (
    id_order_item INT AUTO_INCREMENT PRIMARY KEY,
    id_order INT NOT NULL,
    id_part INT NOT NULL,
    sku VARCHAR(80) NOT NULL,
    part_name VARCHAR(160) NOT NULL,
    unit_price_minor BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT ck_store_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT fk_store_order_item_order FOREIGN KEY (id_order) REFERENCES tb_store_order(id_order),
    CONSTRAINT fk_store_order_item_part FOREIGN KEY (id_part) REFERENCES tb_car_part(id_part)
);

INSERT INTO tb_car_part
  (sku, name, category, description, price_minor, stock_quantity, active, image_url, created_at, updated_at)
VALUES
('BRK-PAD-001', 'Premium Ceramic Brake Pads', 'Brakes',
 'Low-dust ceramic front brake pad set with quiet-operation shims for common passenger vehicles.',
 6999, 24, TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('BAT-AGM-070', '70Ah AGM Start-Stop Battery', 'Electrical',
 'Maintenance-free AGM battery designed for vehicles with start-stop systems and high electrical demand.',
 15999, 12, TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('FLT-OIL-101', 'Long-Life Oil Filter', 'Filters',
 'High-efficiency oil filter with anti-drainback valve for extended engine protection.',
 1299, 60, TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('FLT-CAB-220', 'Activated Carbon Cabin Filter', 'Filters',
 'Cabin air filter that reduces dust, pollen and common road odors.',
 1899, 42, TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('OIL-5W30-5L', 'Fully Synthetic 5W-30 Engine Oil 5L', 'Fluids',
 'Five-liter fully synthetic engine oil suitable for modern petrol and diesel engines.',
 4899, 35, TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('LGT-H7-PLUS', 'H7 Performance Headlight Pair', 'Lighting',
 'Road-legal halogen headlight pair with brighter, whiter light than standard bulbs.',
 2499, 30, TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WPR-650-400', 'Aerodynamic Wiper Blade Set', 'Exterior',
 'Quiet all-season flat wiper blade set, 650 mm and 400 mm.',
 2799, 28, TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TIR-20555R16', '205/55 R16 All-Season Tire', 'Tires',
 'Balanced all-season touring tire with wet-grip and low-noise tread design.',
 8499, 20, TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SPK-IRIDIUM-4', 'Iridium Spark Plug Set of 4', 'Engine',
 'Long-life iridium spark plugs for reliable ignition and consistent engine performance.',
 3999, 18, TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMG-ROAD-KIT', 'European Roadside Emergency Kit', 'Accessories',
 'Compact safety kit containing warning triangle, reflective vest, first-aid pack and work gloves.',
 3299, 50, TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
