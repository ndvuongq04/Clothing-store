package com.clothingstore.clothing_store_be.dto.cart;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDto {
    private Long id;
    private String voucherCode;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private List<CartItemDto> items;
}
