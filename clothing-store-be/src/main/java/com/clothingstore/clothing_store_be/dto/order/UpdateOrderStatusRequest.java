package com.clothingstore.clothing_store_be.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    @Pattern(regexp = "^(confirmed|shipping|completed|cancelled)$",
             message = "Trạng thái không hợp lệ")
    private String status;

    private String trackingCode;
    private String reason;
}
