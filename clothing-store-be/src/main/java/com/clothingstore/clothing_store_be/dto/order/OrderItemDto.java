package com.clothingstore.clothing_store_be.dto.order;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItemDto {
    private Long orderItemId;
    private Long productId;
    private String variantId;
    private String productName;
    private String color;
    private String size;
    private String thumbnailUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
