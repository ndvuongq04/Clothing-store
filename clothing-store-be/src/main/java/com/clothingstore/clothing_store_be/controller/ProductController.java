package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.product.ProductDetailDto;
import com.clothingstore.clothing_store_be.dto.product.ProductFilterRequest;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.clothingstore.clothing_store_be.enums.ProductColor;
import com.clothingstore.clothing_store_be.enums.ProductSize;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Public search — chỉ trả về sản phẩm visible (status=1) và chưa bị xoá mềm
     */
    @GetMapping
    public ResponseEntity<ResultPaginationDTO> search(
            @ModelAttribute ProductFilterRequest filter) {
        return ResponseEntity.ok(productService.search(filter, false));
    }

    /**
     * Public product detail — chỉ trả về nếu status=1 và chưa bị xoá mềm
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    /**
     * Lấy danh sách màu sắc chuẩn
     */
    @GetMapping("/attributes/colors")
    public ResponseEntity<List<Map<String, String>>> getColors() {
        return ResponseEntity.ok(Arrays.stream(ProductColor.values())
                .map(color -> Map.of("code", color.name(), "name", color.getDisplayName()))
                .toList());
    }

    /**
     * Lấy danh sách kích thước chuẩn
     */
    @GetMapping("/attributes/sizes")
    public ResponseEntity<List<Map<String, String>>> getSizes() {
        return ResponseEntity.ok(Arrays.stream(ProductSize.values())
                .map(size -> Map.of("code", size.name(), "name", size.getDisplayName()))
                .toList());
    }
}
