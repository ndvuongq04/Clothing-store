package com.clothingstore.clothing_store_be.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderDetailDto {
    private Long orderId;
    private String orderCode;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private String voucherCode;
    private String note;
    private String trackingCode;
    private String cancelReason;
    // Địa chỉ từ snapshot
    private String recipientName;
    private String recipientPhone;
    private String addressLine;
    private List<OrderItemDto> items;
    private PaymentDto payment;
    private String paymentUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
