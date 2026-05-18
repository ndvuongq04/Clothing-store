package com.clothingstore.clothing_store_be.dto.statistic;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSaleData {
    private Long productId;
    private String name;
    private String thumbnailUrl;
    private String categoryName;
    private long quantitySold;
    private BigDecimal revenue;
}
