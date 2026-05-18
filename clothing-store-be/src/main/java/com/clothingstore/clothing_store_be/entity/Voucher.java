package com.clothingstore.clothing_store_be.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher {

    @Id
    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    // "percent" | "fixed"
    @Column(name = "type", length = 10, nullable = false)
    private String type;

    @Column(name = "discount_value", nullable = false, precision = 15, scale = 0)
    private BigDecimal discountValue;

    @Builder.Default
    @Column(name = "min_order_value", nullable = false, precision = 15, scale = 0)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    // Chỉ dùng khi type=percent — giới hạn số tiền giảm tối đa
    @Column(name = "max_discount_cap", precision = 15, scale = 0)
    private BigDecimal maxDiscountCap;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "max_usage", nullable = false)
    private Integer maxUsage;

    @Builder.Default
    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Builder.Default
    @Column(name = "active", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
