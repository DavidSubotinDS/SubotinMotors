ALTER TABLE tb_listing_comment
  ADD COLUMN image_file_name VARCHAR(255);

ALTER TABLE tb_listing_comment
  ADD COLUMN image_file_type VARCHAR(100);

ALTER TABLE tb_listing_comment
  ADD COLUMN image_data LONGTEXT;

ALTER TABLE tb_listing_comment
  ADD CONSTRAINT ck_listing_comment_complete_image CHECK (
    (image_file_name IS NULL AND image_file_type IS NULL AND image_data IS NULL)
    OR (image_file_name IS NOT NULL AND image_file_type IS NOT NULL AND image_data IS NOT NULL)
  );
