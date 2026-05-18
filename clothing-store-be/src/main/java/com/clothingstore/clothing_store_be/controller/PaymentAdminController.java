package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.order.*;
import com.clothingstore.clothing_store_be.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final OrderService orderService;

    @GetMapping("/settings")
    public ResponseEntity<List<PaymentSettingDto>> getSettings() {
        return ResponseEntity.ok(orderService.getPaymentSettings());
    }

    @PatchMapping("/settings/{method}/toggle")
    public ResponseEntity<PaymentSettingDto> toggle(@PathVariable("method") String method) {
        return ResponseEntity.ok(orderService.togglePaymentMethod(method));
    }

    @GetMapping("/reconcile")
    public ResponseEntity<List<ReconcileResultDto>> reconcile(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(orderService.reconcile(date));
    }

    @PostMapping("/refund")
    public ResponseEntity<RefundResponseDto> refundByError(
            @RequestBody @Valid AdminRefundRequestDto req,
            HttpServletRequest httpReq) {
        // Lấy username từ security context
        String adminUsername = getCurrentUsername();
        return ResponseEntity.ok(orderService.refundByError(req, adminUsername, httpReq));
    }

    private String getCurrentUsername() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        return "admin";
    }
}
