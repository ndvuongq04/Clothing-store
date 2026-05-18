package com.clothingstore.clothing_store_be.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlaceOrderRequest {

    @NotNull(message = "Vui lòng chọn địa chỉ giao hàng")
    private Long addressId;

    @NotEmpty(message = "Vui lòng chọn ít nhất một sản phẩm để đặt hàng")
    private List<Long> cartItemIds;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    @Pattern(regexp = "^(cod|vnpay)$", message = "Phương thức thanh toán phải là 'cod' hoặc 'vnpay'")
    private String paymentMethod;

    private String voucherCode;

    private String note;
}
