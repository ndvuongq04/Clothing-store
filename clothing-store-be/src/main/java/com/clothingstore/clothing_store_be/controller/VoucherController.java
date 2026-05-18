package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.cart.ApplyVoucherRequest;
import com.clothingstore.clothing_store_be.dto.cart.CartDto;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.service.CartService;
import com.clothingstore.clothing_store_be.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vouchers")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(voucherService.getAllActive(page, pageSize));
    }

    @PostMapping("/apply")
    public ResponseEntity<CartDto> applyVoucher(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ApplyVoucherRequest req) {
        return ResponseEntity.ok(cartService.applyVoucher(userId(jwt), req.getVoucherCode()));
    }

    @DeleteMapping("/remove")
    public ResponseEntity<CartDto> removeVoucher(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(cartService.removeVoucher(userId(jwt)));
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
