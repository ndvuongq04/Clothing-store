package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.product.CategoryCreateRequest;
import com.clothingstore.clothing_store_be.dto.product.CategoryDto;
import com.clothingstore.clothing_store_be.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // ─── Public endpoints ────────────────────────────────

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllTree() {
        return ResponseEntity.ok(categoryService.getAllTree());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(categoryService.getById(id));
    }

    // ─── Admin endpoints ─────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDto> create(
            @RequestBody @Valid CategoryCreateRequest req) {
        return ResponseEntity.ok(categoryService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDto> update(
            @PathVariable("id") Long id,
            @RequestBody @Valid CategoryCreateRequest req) {
        return ResponseEntity.ok(categoryService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> delete(@PathVariable("id") Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok("Xoá danh mục thành công");
    }
}
