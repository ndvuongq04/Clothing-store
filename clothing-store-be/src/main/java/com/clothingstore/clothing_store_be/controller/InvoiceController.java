package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.invoice.InvoiceDetailDto;
import com.clothingstore.clothing_store_be.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * GET /invoices/order/{orderId} — Xem thông tin hóa đơn của đơn hàng (user)
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<InvoiceDetailDto> getMyInvoice(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(invoiceService.getInvoiceByOrderId(userId(jwt), orderId));
    }

    /**
     * GET /invoices/order/{orderId}/pdf — Tải PDF hóa đơn (chỉ khi đã thanh toán)
     */
    @GetMapping("/order/{orderId}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("orderId") Long orderId) {
        byte[] pdf = invoiceService.downloadInvoicePdf(userId(jwt), orderId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-order-" + orderId + ".pdf");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
