package com.clothingstore.clothing_store_be.dto.invoice;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDto {
    private Long invoiceId;
    private String invoiceCode;
    private Long orderId;
    private String orderCode;
    private String status;           // PENDING, PAID, REFUNDED, CANCELLED
    private String customerName;
    private String customerEmail;
    private String paymentMethod;
    private String paymentStatus;
    private LocalDate issuedDate;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String fileUrl;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
