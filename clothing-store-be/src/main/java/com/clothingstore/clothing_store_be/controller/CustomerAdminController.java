package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.customer.CustomerDetailDto;
import com.clothingstore.clothing_store_be.dto.customer.CustomerFilterRequest;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CustomerAdminController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<ResultPaginationDTO> getCustomers(@ModelAttribute CustomerFilterRequest req) {
        return ResponseEntity.ok(customerService.getCustomers(req));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<CustomerDetailDto> getCustomerDetail(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(customerService.getCustomerDetail(userId));
    }
}
