ALTER TABLE tb_store_order
  ADD COLUMN shipping_street_address VARCHAR(255);

ALTER TABLE tb_store_order
  ADD COLUMN shipping_city VARCHAR(120);

ALTER TABLE tb_store_order
  ADD COLUMN shipping_postal_code VARCHAR(30);

ALTER TABLE tb_store_order
  ADD COLUMN shipping_country VARCHAR(120);

UPDATE tb_store_order
SET shipping_street_address = shipping_address,
    shipping_city = shipping_address,
    shipping_postal_code = '00000',
    shipping_country = 'Unspecified'
WHERE shipping_address IS NOT NULL
  AND TRIM(shipping_address) <> '';
