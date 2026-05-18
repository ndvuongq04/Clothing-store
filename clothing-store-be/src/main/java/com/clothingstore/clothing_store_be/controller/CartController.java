package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.cart.AddToCartRequest;
import com.clothingstore.clothing_store_be.dto.cart.CartDto;
import com.clothingstore.clothing_store_be.dto.cart.UpdateCartItemRequest;
import com.clothingstore.clothing_store_be.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartDto> getCart(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cartService.getCart(userId(jwt)));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid AddToCartRequest req) {
        return ResponseEntity.ok(cartService.addItem(userId(jwt), req));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("itemId") Long itemId,
            @RequestBody @Valid UpdateCartItemRequest req) {
        return ResponseEntity.ok(cartService.updateItem(userId(jwt), itemId, req));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("itemId") Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(userId(jwt), itemId));
    }

    @DeleteMapping
    public ResponseEntity<CartDto> clearCart(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cartService.clearCart(userId(jwt)));
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
