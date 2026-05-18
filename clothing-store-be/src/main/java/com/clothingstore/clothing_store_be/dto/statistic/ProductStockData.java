package com.clothingstore.clothing_store_be.dto.statistic;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStockData {
    private Long productId;
    private String name;
    private String thumbnailUrl;
    private String categoryName;
    private long stockQty;
}
