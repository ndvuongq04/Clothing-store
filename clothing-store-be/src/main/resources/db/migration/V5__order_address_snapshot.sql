-- V5: Add address snapshot columns to orders
ALTER TABLE orders
    ADD COLUMN ship_full_name VARCHAR(100) NULL AFTER address_id,
    ADD COLUMN ship_phone     VARCHAR(15)  NULL AFTER ship_full_name,
    ADD COLUMN ship_province  VARCHAR(100) NULL AFTER ship_phone,
    ADD COLUMN ship_district  VARCHAR(100) NULL AFTER ship_province,
    ADD COLUMN ship_ward      VARCHAR(100) NULL AFTER ship_district,
    ADD COLUMN ship_street    VARCHAR(255) NULL AFTER ship_ward;
