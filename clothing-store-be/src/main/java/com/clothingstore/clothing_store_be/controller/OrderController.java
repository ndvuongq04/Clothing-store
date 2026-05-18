package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.order.*;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderDetailDto> placeOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid PlaceOrderRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(userId(jwt), req, httpReq));
    }

    @PostMapping("/{orderId}/payment")
    public ResponseEntity<Object> choosePayment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("orderId") Long orderId,
            @RequestBody @Valid ChoosePaymentRequest req,
            HttpServletRequest httpReq) {
        return ResponseEntity.ok(orderService.choosePayment(userId(jwt), orderId, req, httpReq));
    }

    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @ModelAttribute OrderFilterRequest req) {
        return ResponseEntity.ok(orderService.getMyOrders(userId(jwt), req));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailDto> getOrderDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(orderService.getOrderDetail(userId(jwt), orderId));
    }

    @GetMapping("/{orderId}/return-request")
    public ResponseEntity<ReturnRequestInfoDto> getMyReturnRequestInfo(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("orderId") Long orderId) {
        return ResponseEntity.ok(orderService.getMyReturnRequestInfo(userId(jwt), orderId));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderDetailDto> cancelOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("orderId") Long orderId,
            @RequestBody(required = false) CancelOrderRequest req) {
        return ResponseEntity.ok(orderService.cancelByUser(userId(jwt), orderId,
                req != null ? req : new CancelOrderRequest()));
    }

    @PostMapping(value = "/{orderId}/return-request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrderDetailDto> requestReturn(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("orderId") Long orderId,
            @RequestParam("reason") String reason,
            @RequestParam(value = "refundBankInfo", required = false) String refundBankInfo,
            @RequestPart("images") List<MultipartFile> images) {
        ReturnRequestDto req = new ReturnRequestDto();
        req.setReason(reason);
        req.setRefundBankInfo(refundBankInfo);
        return ResponseEntity.ok(orderService.requestReturn(userId(jwt), orderId, req, images));
    }

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
