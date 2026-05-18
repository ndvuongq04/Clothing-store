package com.clothingstore.clothing_store_be.dto.customer;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerSummaryDto {
    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String gender;
    private boolean status;
    private boolean emailVerified;
    private long totalOrders;
}
