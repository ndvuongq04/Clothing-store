package com.clothingstore.clothing_store_be.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminRefundRequestDto {
    @NotBlank(message = "Mã đơn hàng không được để trống")
    private String orderCode;

    @NotNull(message = "Số tiền hoàn không được để trống")
    @Positive(message = "Số tiền hoàn phải lớn hơn 0")
    private BigDecimal amount;

    @NotBlank(message = "Lý do hoàn tiền không được để trống")
    private String reason;
}
