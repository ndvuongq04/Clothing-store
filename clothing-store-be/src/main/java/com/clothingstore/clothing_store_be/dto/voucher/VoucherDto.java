package com.clothingstore.clothing_store_be.dto.voucher;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherDto {
    private String voucherCode;
    private String type;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountCap;
    private LocalDate startDate;
    private LocalDate expiryDate;
    private Integer maxUsage;
    private Integer usedCount;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}
