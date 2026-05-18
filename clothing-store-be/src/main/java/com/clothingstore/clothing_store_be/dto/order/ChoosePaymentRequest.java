package com.clothingstore.clothing_store_be.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ChoosePaymentRequest {

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    @Pattern(regexp = "^(cod|vnpay)$", message = "Phương thức thanh toán phải là 'cod' hoặc 'vnpay'")
    private String paymentMethod;
}
