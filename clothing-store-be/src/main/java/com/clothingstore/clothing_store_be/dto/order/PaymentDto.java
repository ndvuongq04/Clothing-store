package com.clothingstore.clothing_store_be.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentDto {
    private Long paymentId;
    private String method;
    private String status;
    private BigDecimal amount;
    private String vnpayTransactionNo;
    private LocalDateTime paidAt;
}
