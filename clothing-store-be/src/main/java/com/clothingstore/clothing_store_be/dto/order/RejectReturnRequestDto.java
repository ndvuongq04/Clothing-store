package com.clothingstore.clothing_store_be.dto.order;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RejectReturnRequestDto {
    @NotBlank(message = "Lý do từ chối không được để trống")
    private String reason;
}
