package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.address.AddressCreateRequest;
import com.clothingstore.clothing_store_be.dto.address.AddressDto;
import com.clothingstore.clothing_store_be.dto.address.AddressUpdateRequest;
import com.clothingstore.clothing_store_be.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressDto>> getAll(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(addressService.getAll(userId(jwt)));
    }

    @PostMapping
    public ResponseEntity<AddressDto> create(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid AddressCreateRequest req) {
        return ResponseEntity.ok(addressService.create(userId(jwt), req));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<AddressDto> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("addressId") Long addressId,
            @RequestBody @Valid AddressUpdateRequest req) {
        return ResponseEntity.ok(addressService.update(userId(jwt), addressId, req));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<String> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("addressId") Long addressId) {
        addressService.delete(userId(jwt), addressId);
        return ResponseEntity.ok("Xóa địa chỉ thành công");
    }

    @PatchMapping("/{addressId}/default")
    public ResponseEntity<AddressDto> setDefault(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("addressId") Long addressId) {
        return ResponseEntity.ok(addressService.setDefault(userId(jwt), addressId));
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
