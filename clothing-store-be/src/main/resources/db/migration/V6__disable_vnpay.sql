-- V6: Tạm thời tắt VNPay, chỉ dùng COD
UPDATE payment_settings SET is_enabled = 0 WHERE method = 'vnpay';
