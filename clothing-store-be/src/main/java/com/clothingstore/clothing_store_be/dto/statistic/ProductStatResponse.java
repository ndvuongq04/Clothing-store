package com.clothingstore.clothing_store_be.dto.statistic;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductStatResponse {
    private List<ProductSaleData> topSelling;
    private List<ProductStockData> slowMoving;
}
