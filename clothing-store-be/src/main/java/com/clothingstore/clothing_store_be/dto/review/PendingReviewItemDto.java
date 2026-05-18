package com.clothingstore.clothing_store_be.dto.review;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingReviewItemDto {
    private Long orderItemId;
    private Long orderId;
    private String orderCode;
    private Long productId;
    private String variantId;
    private String productName;
    private String color;
    private String size;
    private String thumbnailUrl;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private LocalDateTime completedAt;
}
