package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.cart.AddToCartRequest;
import com.clothingstore.clothing_store_be.dto.cart.CartDto;
import com.clothingstore.clothing_store_be.dto.cart.UpdateCartItemRequest;

public interface CartService {

    CartDto getCart(Long userId);

    CartDto addItem(Long userId, AddToCartRequest req);

    CartDto updateItem(Long userId, Long itemId, UpdateCartItemRequest req);

    CartDto removeItem(Long userId, Long itemId);

    CartDto clearCart(Long userId);

    CartDto applyVoucher(Long userId, String voucherCode);

    CartDto removeVoucher(Long userId);
}
