-- V3: Add soft-delete support to vouchers
ALTER TABLE vouchers
    ADD COLUMN deleted_at DATETIME NULL DEFAULT NULL;
