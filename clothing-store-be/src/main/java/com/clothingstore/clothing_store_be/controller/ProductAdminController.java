package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.product.*;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.service.ProductImportService;
import com.clothingstore.clothing_store_be.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductService productService;
    private final ProductImportService productImportService;

    /**
     * Admin search — bao gồm cả sản phẩm ẩn (status=0)
     */
    @GetMapping
    public ResponseEntity<ResultPaginationDTO> search(
            @ModelAttribute ProductFilterRequest filter) {
        return ResponseEntity.ok(productService.search(filter, true));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(productService.getByIdAdmin(id));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDetailDto> create(
            @RequestPart("product") @Valid ProductCreateRequest req,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "images", required = false) java.util.List<MultipartFile> images) {
        return ResponseEntity.ok(productService.create(req, thumbnail, images));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductDetailDto> update(
            @PathVariable("id") Long id,
            @RequestPart("product") @Valid ProductUpdateRequest req,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestPart(value = "images", required = false) java.util.List<MultipartFile> newImages,
            @RequestParam(value = "removeImageUrls", required = false) java.util.List<String> removeImageUrls) {
        return ResponseEntity.ok(productService.update(id, req, thumbnail, newImages, removeImageUrls));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> softDelete(@PathVariable("id") Long id) {
        productService.softDelete(id);
        return ResponseEntity.ok("Xoá sản phẩm thành công");
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<String> toggleVisibility(@PathVariable("id") Long id) {
        productService.toggleVisibility(id);
        return ResponseEntity.ok("Đã thay đổi trạng thái hiển thị");
    }

    // ─── Import endpoints ───────────────────────────────

    /**
     * Tải file Excel mẫu để import sản phẩm hàng loạt
     */
    @GetMapping("/import/template")
    public ResponseEntity<InputStreamResource> downloadImportTemplate() {
        ByteArrayInputStream template = productImportService.generateImportTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import_san_pham_mau.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(template));
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultDto> bulkImport(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productImportService.bulkImport(file));
    }

    // ─── Variant sub-resource endpoints ──────────────────

    @GetMapping("/{productId}/variants")
    public ResponseEntity<ResultPaginationDTO> getVariantsByProductId(
            @PathVariable("productId") Long productId,
            @ModelAttribute VariantFilterRequest filter) {
        return ResponseEntity.ok(productService.getVariantsByProductId(productId, filter));
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<VariantDto> addVariant(
            @PathVariable("productId") Long productId,
            @RequestBody @Valid VariantCreateRequest req) {
        return ResponseEntity.ok(productService.addVariant(productId, req));
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<VariantDto> updateVariant(
            @PathVariable("productId") Long productId,
            @PathVariable("variantId") String variantId,
            @RequestBody @Valid VariantCreateRequest req) {
        return ResponseEntity.ok(productService.updateVariant(productId, variantId, req));
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<String> deleteVariant(
            @PathVariable("productId") Long productId,
            @PathVariable("variantId") String variantId) {
        productService.deleteVariant(productId, variantId);
        return ResponseEntity.ok("Xóa biến thể thành công");
    }

    @PatchMapping("/{productId}/variants/{variantId}/stock")
    public ResponseEntity<String> updateVariantStock(
            @PathVariable("productId") Long productId,
            @PathVariable("variantId") String variantId,
            @RequestParam("stockQty") Integer stockQty) {
        productService.updateVariantStock(variantId, stockQty);
        return ResponseEntity.ok("Cập nhật tồn kho thành công");
    }
}
