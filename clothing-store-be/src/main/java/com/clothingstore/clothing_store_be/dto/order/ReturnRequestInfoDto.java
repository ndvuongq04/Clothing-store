package com.clothingstore.clothing_store_be.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequestInfoDto {
    private Long orderId;
    private String orderCode;
    private String orderStatus;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal orderTotal;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String refundReason;
    private String refundBankInfo;
    private LocalDateTime refundRequestDate;
    private BigDecimal refundAmount;
    private LocalDateTime refundApprovedAt;
    private String refundApprovedBy;
    private LocalDateTime refundRejectedAt;
    private String refundRejectedBy;
    private String refundRejectReason;
    private String refundVnpayResponseCode;
    private String refundVnpayTransactionNo;
    private String refundTransferProofUrl;
    private List<String> imageUrls;
}
