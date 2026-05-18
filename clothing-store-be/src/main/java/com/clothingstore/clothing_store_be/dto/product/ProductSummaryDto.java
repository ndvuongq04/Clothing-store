package com.clothingstore.clothing_store_be.dto.product;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSummaryDto {
    private Long id;
    private String name;
    private String thumbnailUrl;
    private BigDecimal basePrice;
    private Integer status;
    private String categoryName;
}
