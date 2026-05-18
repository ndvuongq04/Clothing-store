package com.clothingstore.clothing_store_be.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequestDto {
    @NotBlank(message = "Ly do hoan hang khong duoc de trong")
    private String reason;

    @Size(max = 255, message = "Thong tin ngan hang nhan hoan tien khong duoc vuot qua 255 ky tu")
    private String refundBankInfo;
}
