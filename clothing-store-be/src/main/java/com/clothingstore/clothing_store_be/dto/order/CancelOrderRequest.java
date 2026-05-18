package com.clothingstore.clothing_store_be.dto.order;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CancelOrderRequest {
    private String reason;
}
