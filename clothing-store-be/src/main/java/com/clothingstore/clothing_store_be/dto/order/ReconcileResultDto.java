package com.clothingstore.clothing_store_be.dto.order;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReconcileResultDto {
    private String orderCode;
    private String localPaymentStatus;
    private String vnpayTransactionNo;
    private boolean matched;
}
