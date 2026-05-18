package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.dto.voucher.VoucherCreateRequest;
import com.clothingstore.clothing_store_be.dto.voucher.VoucherDto;
import com.clothingstore.clothing_store_be.dto.voucher.VoucherUpdateRequest;
import com.clothingstore.clothing_store_be.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/vouchers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class VoucherAdminController {

    private final VoucherService voucherService;

    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getAll(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return ResponseEntity.ok(voucherService.getAll(page, pageSize));
    }

    @GetMapping("/{voucherCode}")
    public ResponseEntity<VoucherDto> getByCode(
            @PathVariable("voucherCode") String voucherCode) {
        return ResponseEntity.ok(voucherService.getByCode(voucherCode));
    }

    @PostMapping
    public ResponseEntity<VoucherDto> create(
            @RequestBody @Valid VoucherCreateRequest req) {
        return ResponseEntity.ok(voucherService.create(req));
    }

    @PutMapping("/{voucherCode}")
    public ResponseEntity<VoucherDto> update(
            @PathVariable("voucherCode") String voucherCode,
            @RequestBody @Valid VoucherUpdateRequest req) {
        return ResponseEntity.ok(voucherService.update(voucherCode, req));
    }

    @DeleteMapping("/{voucherCode}")
    public ResponseEntity<String> delete(
            @PathVariable("voucherCode") String voucherCode) {
        voucherService.delete(voucherCode);
        return ResponseEntity.ok("Xoá voucher thành công");
    }

    @PatchMapping("/{voucherCode}/toggle")
    public ResponseEntity<VoucherDto> toggleActive(
            @PathVariable("voucherCode") String voucherCode) {
        return ResponseEntity.ok(voucherService.toggleActive(voucherCode));
    }
}
