package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.product.*;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {

    ResultPaginationDTO search(ProductFilterRequest filter, boolean includeHidden);

    ProductDetailDto getById(Long id);

    ProductDetailDto getByIdAdmin(Long id);

    ProductDetailDto create(ProductCreateRequest req, MultipartFile thumbnail, List<MultipartFile> images);

    ProductDetailDto update(Long id, ProductUpdateRequest req, MultipartFile thumbnail, List<MultipartFile> newImages,
            List<String> removeImageUrls);

    void softDelete(Long id);

    void toggleVisibility(Long id);

    // ─── Variant sub-resource APIs ───────────────────────

    VariantDto addVariant(Long productId, VariantCreateRequest req);

    VariantDto updateVariant(Long productId, String variantId, VariantCreateRequest req);

    void deleteVariant(Long productId, String variantId);

    void updateVariantStock(String variantId, Integer stockQty);

    ResultPaginationDTO getVariantsByProductId(Long productId, VariantFilterRequest filter);
}
