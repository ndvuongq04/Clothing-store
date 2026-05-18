package com.clothingstore.clothing_store_be.dto.order;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentSettingDto {
    private String method;
    private boolean isEnabled;
}
