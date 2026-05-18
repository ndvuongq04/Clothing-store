package com.clothingstore.clothing_store_be.dto.invoice;

import com.clothingstore.clothing_store_be.dto.order.OrderItemDto;
import com.clothingstore.clothing_store_be.dto.order.PaymentDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDetailDto {

    // ─── Thông tin hóa đơn ────────────────────────────────
    private Long invoiceId;
    private String invoiceCode;
    private String status;              // PENDING, PAID, REFUNDED, CANCELLED
    private LocalDate issuedDate;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String fileUrl;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Thông tin khách hàng ─────────────────────────────
    private CustomerInfo customer;

    // ─── Thông tin đơn hàng ───────────────────────────────
    private OrderInfo order;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CustomerInfo {
        private Long userId;
        private String fullName;
        private String email;
        private String phone;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OrderInfo {
        private Long orderId;
        private String orderCode;
        private String orderStatus;
        private String paymentMethod;
        private String paymentStatus;
        private String voucherCode;
        private String note;
        private String trackingCode;

        // Địa chỉ giao hàng
        private String recipientName;
        private String recipientPhone;
        private String shipStreet;
        private String shipWard;
        private String shipDistrict;
        private String shipProvince;

        // Thanh toán
        private PaymentDto payment;

        // Danh sách sản phẩm
        private List<OrderItemDto> items;

        private LocalDateTime orderCreatedAt;
    }
}
