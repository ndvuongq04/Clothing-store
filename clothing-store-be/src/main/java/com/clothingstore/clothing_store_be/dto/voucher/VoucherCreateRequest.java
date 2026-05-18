package com.clothingstore.clothing_store_be.dto.voucher;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherCreateRequest {

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 50, message = "Mã voucher tối đa 50 ký tự")
    private String voucherCode;

    @NotBlank(message = "Loại giảm giá không được để trống")
    @Pattern(regexp = "percent|fixed", message = "Loại giảm giá phải là 'percent' hoặc 'fixed'")
    private String type;

    @NotNull(message = "Giá trị giảm không được để trống")
    @Positive(message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    private BigDecimal minOrderValue;

    private BigDecimal maxDiscountCap;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày hết hạn không được để trống")
    private LocalDate expiryDate;

    @NotNull(message = "Số lượt dùng tối đa không được để trống")
    @Min(value = 1, message = "Số lượt dùng tối đa phải lớn hơn 0")
    private Integer maxUsage;
}
