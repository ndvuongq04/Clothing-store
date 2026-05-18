package com.clothingstore.clothing_store_be.dto.product;

import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantCreateRequest {
    private String color;
    private String size;

    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stockQty;

    private BigDecimal salePrice;

    private BigDecimal importPrice;
}
