-- Native browser harness only: model the completed V19 identity boundary while
-- retaining legacy test entities for the ordinary H2 unit fixture.
ALTER TABLE tb_car DROP CONSTRAINT fk_car_user;
ALTER TABLE tb_car_bid DROP CONSTRAINT fk_bid_user;
ALTER TABLE tb_test_drive DROP CONSTRAINT fk_test_drive_user;
ALTER TABLE tb_payment_account DROP CONSTRAINT fk_payment_account_user;
ALTER TABLE tb_payment_order DROP CONSTRAINT fk_payment_order_buyer;
ALTER TABLE tb_payment_order DROP CONSTRAINT fk_payment_order_seller;
ALTER TABLE tb_cart_item DROP CONSTRAINT fk_cart_item_user;
ALTER TABLE tb_store_order DROP CONSTRAINT fk_store_order_user;
ALTER TABLE tb_auction_follow DROP CONSTRAINT fk_auction_follow_user;
ALTER TABLE tb_auction_notification DROP CONSTRAINT fk_notification_user;
ALTER TABLE tb_listing_comment DROP CONSTRAINT fk_listing_comment_user;
ALTER TABLE tb_car_listing DROP CONSTRAINT fk_car_listing_seller;
ALTER TABLE tb_listing_test_ride DROP CONSTRAINT fk_listing_test_ride_user;
ALTER TABLE tb_listing_deposit DROP CONSTRAINT fk_listing_deposit_buyer;
