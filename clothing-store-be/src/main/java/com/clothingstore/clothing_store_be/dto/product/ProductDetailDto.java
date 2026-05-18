package com.clothingstore.clothing_store_be.dto.product;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailDto {
    private Long id;
    private String name;
    private String description;
    private String thumbnailUrl;
    private BigDecimal basePrice;
    private Integer status;
    private CategoryDto category;
    private List<VariantDto> variants;
    private List<String> imageUrls;
}
