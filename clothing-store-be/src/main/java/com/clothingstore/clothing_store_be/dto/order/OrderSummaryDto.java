package com.clothingstore.clothing_store_be.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderSummaryDto {
    private Long orderId;
    private String orderCode;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal total;
    private int itemCount;
    private String customerName;
    private String customerEmail;
    private LocalDateTime createdAt;
}
