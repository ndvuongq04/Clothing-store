package com.clothingstore.clothing_store_be.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundResponseDto {
    private String orderCode;
    private String paymentStatus;
    private String orderStatus;
    private BigDecimal refundAmount;
    private String refundReason;
    private String refundBankInfo;
    private LocalDateTime refundRequestDate;
    private LocalDateTime refundApprovedAt;
    private String refundApprovedBy;
    private String refundVnpayResponseCode;
    private String refundVnpayTransactionNo;
    private String refundTransferProofUrl;
}
