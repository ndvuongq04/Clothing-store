package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.order.*;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class OrderAdminController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getAllOrders(@ModelAttribute OrderFilterRequest req) {
        return ResponseEntity.ok(orderService.getAllOrders(req));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailDto> getOrderDetail(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDetailAdmin(orderId));
    }

    @GetMapping("/{orderId}/return-request")
    public ResponseEntity<ReturnRequestInfoDto> getReturnRequestInfo(@PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(orderService.getReturnRequestInfo(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderDetailDto> updateStatus(
            @PathVariable("orderId") Long orderId,
            @RequestBody @Valid UpdateOrderStatusRequest req) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, req));
    }

    @PostMapping("/{orderId}/confirm-return")
    public ResponseEntity<ReturnRequestInfoDto> confirmReturnRequest(
            @PathVariable("orderId") Long orderId,
            @RequestParam(value = "note", required = false) String note) {
        return ResponseEntity.ok(orderService.confirmReturnRequest(orderId, note));
    }

    @PostMapping(value = "/{orderId}/refund-return", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RefundResponseDto> refundReturnedOrder(
            @PathVariable("orderId") Long orderId,
            @RequestParam(value = "note", required = false) String note,
            @RequestPart(value = "billImage", required = false) MultipartFile billImage,
            HttpServletRequest httpReq) {
        return ResponseEntity.ok(orderService.refundReturnedOrder(orderId, note, billImage, httpReq));
    }

    @PostMapping("/{orderId}/reject-return")
    public ResponseEntity<ReturnRequestInfoDto> rejectReturnRequest(
            @PathVariable("orderId") Long orderId,
            @RequestBody @Valid RejectReturnRequestDto req) {
        return ResponseEntity.ok(orderService.rejectReturnRequest(orderId, req));
    }
}
