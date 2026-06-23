ALTER TABLE tb_user_profile
  ADD COLUMN street_address VARCHAR(255);

ALTER TABLE tb_user_profile
  ADD COLUMN city VARCHAR(120);

ALTER TABLE tb_user_profile
  ADD COLUMN postal_code VARCHAR(30);

ALTER TABLE tb_user_profile
  ADD COLUMN country VARCHAR(120);

-- Preserve checkout compatibility for existing profiles. Users can replace these
-- migrated placeholders with a precise shipping address from account settings.
UPDATE tb_user_profile
SET street_address = address,
    city = address,
    postal_code = '00000',
    country = 'Unspecified'
WHERE address IS NOT NULL
  AND TRIM(address) <> '';

ALTER TABLE tb_user_profile
  MODIFY COLUMN address VARCHAR(255) NULL;
