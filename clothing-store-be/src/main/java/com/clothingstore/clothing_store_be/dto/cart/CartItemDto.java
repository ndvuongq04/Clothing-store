package com.clothingstore.clothing_store_be.dto.cart;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDto {
    private Long id;
    private String variantId;
    private Long productId;
    private String productName;
    private String thumbnailUrl;
    private String color;
    private String size;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
