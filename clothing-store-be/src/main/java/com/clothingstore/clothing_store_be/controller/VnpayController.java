package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.order.OrderDetailDto;
import com.clothingstore.clothing_store_be.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payment/vnpay")
@RequiredArgsConstructor
@Slf4j
public class VnpayController {

    private final OrderService orderService;

    // VNPay gọi endpoint này sau khi xử lý thanh toán (server-to-server)
    @GetMapping(value = "/ipn", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> ipn(@RequestParam Map<String, String> params) {
        log.info("VNPay IPN received: {}", params);
        return ResponseEntity.ok(orderService.handleVnpayIpn(params));
    }

    // Redirect user về sau khi thanh toán xong
    @GetMapping("/return")
    public ResponseEntity<OrderDetailDto> vnpayReturn(@RequestParam Map<String, String> params) {
        log.info("VNPay return received: {}", params);
        return ResponseEntity.ok(orderService.handleVnpayReturn(params));
    }
}
