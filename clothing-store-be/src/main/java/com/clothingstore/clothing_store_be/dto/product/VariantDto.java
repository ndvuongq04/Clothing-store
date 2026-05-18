package com.clothingstore.clothing_store_be.dto.product;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantDto {
    private String id;
    private String color;
    private String size;
    private String sku;
    private Integer stockQty;
    private BigDecimal salePrice;

    private BigDecimal importPrice;
}
