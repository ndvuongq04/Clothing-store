package com.clothingstore.clothing_store_be.dto.product;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFilterRequest {
    private String keyword;
    private Long categoryId;
    private List<Long> categoryIds;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String color;
    private List<String> colors;
    private String size;
    private List<String> sizes;
    private Boolean inStock;
    private Integer status;
    private String sortBy;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int pageSize = 20;
}
