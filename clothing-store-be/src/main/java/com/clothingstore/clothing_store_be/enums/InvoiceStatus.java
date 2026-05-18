package com.clothingstore.clothing_store_be.enums;

public enum InvoiceStatus {
    PENDING,    // Đặt hàng xong, chưa thanh toán (COD hoặc đang chờ)
    PAID,       // Đã thanh toán thành công
    REFUNDED,   // Đã hoàn tiền
    CANCELLED   // Đơn bị huỷ
}
