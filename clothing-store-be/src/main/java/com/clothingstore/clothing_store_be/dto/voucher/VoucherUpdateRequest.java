package com.clothingstore.clothing_store_be.dto.voucher;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUpdateRequest {

    @Pattern(regexp = "percent|fixed", message = "Loại giảm giá phải là 'percent' hoặc 'fixed'")
    private String type;

    @Positive(message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    private BigDecimal minOrderValue;

    private BigDecimal maxDiscountCap;

    private LocalDate startDate;

    private LocalDate expiryDate;

    @Min(value = 1, message = "Số lượt dùng tối đa phải lớn hơn 0")
    private Integer maxUsage;

    private Boolean active;
}
