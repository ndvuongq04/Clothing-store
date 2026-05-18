package com.clothingstore.clothing_store_be.dto.customer;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CustomerFilterRequest {
    private String keyword; // tìm theo tên hoặc email
    private Boolean status; // null = tất cả, true = đang hoạt động, false = bị khoá
    private int page = 0;
    private int pageSize = 20;
}
