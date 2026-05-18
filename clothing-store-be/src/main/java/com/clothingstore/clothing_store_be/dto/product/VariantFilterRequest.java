package com.clothingstore.clothing_store_be.dto.product;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantFilterRequest {
    private String color;
    private String size;
    private String sortBy; // color, size, stock_asc, stock_desc, price_asc, price_desc

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int pageSize = 20;
}
