package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.dto.product.*;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.entity.*;
import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.repository.*;
import com.clothingstore.clothing_store_be.service.FileStorageService;
import com.clothingstore.clothing_store_be.service.ProductService;
import com.clothingstore.clothing_store_be.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_IMG_FOLDER = "products";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderItemRepository orderItemRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO search(ProductFilterRequest filter, boolean includeHidden) {
        ProductFilterRequest effectiveFilter = prepareSearchFilter(filter, includeHidden);
        Sort sort = buildSort(effectiveFilter.getSortBy());
        Pageable pageable = PageRequest.of(effectiveFilter.getPage(), effectiveFilter.getPageSize(), sort);

        Page<Product> page = productRepository.findAll(
                ProductSpecification.buildFilter(effectiveFilter, includeHidden),
                pageable);

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta(
                page.getNumber(),
                page.getSize(),
                page.getTotalPages(),
                page.getTotalElements());

        return new ResultPaginationDTO(meta, page.map(this::toSummaryDto).getContent());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailDto getById(Long id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm với id: " + id));
        return toDetailDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailDto getByIdAdmin(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm với id: " + id));
        return toDetailDto(product);
    }

    @Override
    public ProductDetailDto create(ProductCreateRequest req,
            org.springframework.web.multipart.MultipartFile thumbnail,
            java.util.List<org.springframework.web.multipart.MultipartFile> images) {
        // Validate category
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> AppException.notFound(
                        "Không tìm thấy danh mục với id: " + req.getCategoryId()));

        // Validate variant uniqueness (color + size)
        if (req.getVariants() != null && !req.getVariants().isEmpty()) {
            validateVariantUniqueness(req.getVariants());
        }

        // Upload thumbnail nếu có
        String thumbnailUrl = req.getThumbnailUrl();
        if (thumbnail != null && !thumbnail.isEmpty()) {
            List<String> urls = fileStorageService.uploadFiles(PRODUCT_IMG_FOLDER, List.of(thumbnail));
            thumbnailUrl = urls.getFirst();
        }

        // Build product
        Product product = Product.builder()
                .name(req.getName())
                .category(category)
                .description(req.getDescription())
                .thumbnailUrl(thumbnailUrl)
                .basePrice(req.getBasePrice())
                .status(req.getStatus() != null ? req.getStatus() : 1)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        Product saved = productRepository.save(product);

        // Save variants
        if (req.getVariants() != null) {
            for (VariantCreateRequest v : req.getVariants()) {
                ProductVariant variant = ProductVariant.builder()
                        .product(saved)
                        .color(v.getColor())
                        .size(v.getSize())
                        .sku(generateSku(saved, v.getColor(), v.getSize()))
                        .stockQty(v.getStockQty() != null ? v.getStockQty() : 0)
                        .salePrice(v.getSalePrice())
                        .importPrice(v.getImportPrice())
                        .build();
                saved.getVariants().add(variant);
            }
        }

        // Upload và lưu ảnh sản phẩm
        int sortIndex = 0;
        // Ảnh từ URL truyền sẵn (nếu có)
        if (req.getImageUrls() != null) {
            for (String url : req.getImageUrls()) {
                ProductImage image = ProductImage.builder()
                        .product(saved)
                        .imageUrl(url)
                        .sortOrder(sortIndex++)
                        .build();
                saved.getImages().add(image);
            }
        }
        // Ảnh từ file upload
        if (images != null && !images.isEmpty()) {
            List<String> uploadedUrls = fileStorageService.uploadFiles(PRODUCT_IMG_FOLDER, images);
            for (String url : uploadedUrls) {
                ProductImage image = ProductImage.builder()
                        .product(saved)
                        .imageUrl(url)
                        .sortOrder(sortIndex++)
                        .build();
                saved.getImages().add(image);
            }
        }

        saved = productRepository.save(saved);
        return toDetailDto(saved);
    }

    @Override
    public ProductDetailDto update(Long id, ProductUpdateRequest req,
            org.springframework.web.multipart.MultipartFile thumbnail,
            java.util.List<org.springframework.web.multipart.MultipartFile> newImages,
            java.util.List<String> removeImageUrls) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm với id: " + id));

        // Partial update — chỉ cập nhật field non-null
        if (req.getName() != null) {
            product.setName(req.getName());
        }
        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> AppException.notFound(
                            "Không tìm thấy danh mục với id: " + req.getCategoryId()));
            product.setCategory(category);
        }
        if (req.getDescription() != null) {
            product.setDescription(req.getDescription());
        }
        // Upload thumbnail mới nếu có
        if (thumbnail != null && !thumbnail.isEmpty()) {
            // Xóa thumbnail cũ nếu là file upload
            if (product.getThumbnailUrl() != null && product.getThumbnailUrl().startsWith("/uploads/")) {
                fileStorageService.deleteFile(product.getThumbnailUrl());
            }
            List<String> urls = fileStorageService.uploadFiles(PRODUCT_IMG_FOLDER, List.of(thumbnail));
            product.setThumbnailUrl(urls.getFirst());
        } else if (req.getThumbnailUrl() != null) {
            product.setThumbnailUrl(req.getThumbnailUrl());
        }
        if (req.getBasePrice() != null) {
            product.setBasePrice(req.getBasePrice());
        }
        if (req.getStatus() != null) {
            product.setStatus(req.getStatus());
        }

        // Update variants — cập nhật nếu trùng color+size, thêm mới nếu chưa có.
        // Các variant cũ không nằm trong request được giữ lại (không xóa để tránh lỗi
        // OrderItem FK).
        if (req.getVariants() != null) {
            validateVariantUniqueness(req.getVariants());

            List<ProductVariant> currentVariants = product.getVariants();

            for (VariantCreateRequest vReq : req.getVariants()) {
                Optional<ProductVariant> existing = currentVariants.stream()
                        .filter(cv -> normalize(cv.getColor()).equals(normalize(vReq.getColor()))
                                && normalize(cv.getSize()).equals(normalize(vReq.getSize())))
                        .findFirst();

                if (existing.isPresent()) {
                    ProductVariant v = existing.get();
                    v.setStockQty(vReq.getStockQty() != null ? vReq.getStockQty() : 0);
                    v.setSalePrice(vReq.getSalePrice());
                    v.setImportPrice(vReq.getImportPrice());
                    v.setSku(generateSku(product, vReq.getColor(), vReq.getSize()));
                } else {
                    ProductVariant v = ProductVariant.builder()
                            .product(product)
                            .color(vReq.getColor())
                            .size(vReq.getSize())
                            .sku(generateSku(product, vReq.getColor(), vReq.getSize()))
                            .stockQty(vReq.getStockQty() != null ? vReq.getStockQty() : 0)
                            .salePrice(vReq.getSalePrice())
                            .importPrice(vReq.getImportPrice())
                            .build();
                    product.getVariants().add(v);
                }
            }
        }

        // Xóa ảnh theo URL yêu cầu
        if (removeImageUrls != null && !removeImageUrls.isEmpty()) {
            product.getImages().removeIf(img -> {
                if (removeImageUrls.contains(img.getImageUrl())) {
                    if (img.getImageUrl().startsWith("/uploads/")) {
                        fileStorageService.deleteFile(img.getImageUrl());
                    }
                    return true;
                }
                return false;
            });
            productRepository.flush();
        }

        // Thêm ảnh từ URL (nếu có)
        int sortIndex = product.getImages().stream()
                .mapToInt(ProductImage::getSortOrder).max().orElse(-1) + 1;
        if (req.getImageUrls() != null) {
            for (String url : req.getImageUrls()) {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(url)
                        .sortOrder(sortIndex++)
                        .build();
                product.getImages().add(image);
            }
        }

        // Upload ảnh mới từ file
        if (newImages != null && !newImages.isEmpty()) {
            List<String> uploadedUrls = fileStorageService.uploadFiles(PRODUCT_IMG_FOLDER, newImages);
            for (String url : uploadedUrls) {
                ProductImage image = ProductImage.builder()
                        .product(product)
                        .imageUrl(url)
                        .sortOrder(sortIndex++)
                        .build();
                product.getImages().add(image);
            }
        }

        Product saved = productRepository.save(product);
        return toDetailDto(saved);
    }

    @Override
    public void softDelete(Long id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm với id: " + id));
        product.setDeletedAt(LocalDateTime.now());
        product.setStatus(0);
        productRepository.save(product);
    }

    @Override
    public void toggleVisibility(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm với id: " + id));
        int newStatus = product.getStatus() == 1 ? 0 : 1;
        product.setStatus(newStatus);
        // Khôi phục sp: xóa deletedAt để public API có thể thấy lại
        if (newStatus == 1) {
            product.setDeletedAt(null);
        }
        productRepository.save(product);
    }

    // ─── Variant sub-resource APIs ───────────────────────

    @Override
    public VariantDto addVariant(Long productId, VariantCreateRequest req) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm"));

        // Check uniqueness against existing variants
        boolean exists = product.getVariants().stream()
                .anyMatch(v -> normalize(v.getColor()).equals(normalize(req.getColor()))
                        && normalize(v.getSize()).equals(normalize(req.getSize())));
        if (exists) {
            throw AppException
                    .badRequest("Biến thể với color=" + req.getColor() + ", size=" + req.getSize() + " đã tồn tại");
        }

        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .color(req.getColor())
                .size(req.getSize())
                .sku(generateSku(product, req.getColor(), req.getSize()))
                .stockQty(req.getStockQty() != null ? req.getStockQty() : 0)
                .salePrice(req.getSalePrice())
                .importPrice(req.getImportPrice())
                .build();

        product.getVariants().add(variant);
        productRepository.save(product);

        return VariantDto.builder()
                .id(variant.getId())
                .color(variant.getColor())
                .size(variant.getSize())
                .sku(variant.getSku())
                .stockQty(variant.getStockQty())
                .salePrice(variant.getSalePrice() != null ? variant.getSalePrice() : product.getBasePrice())
                .importPrice(variant.getImportPrice())
                .build();
    }

    @Override
    public VariantDto updateVariant(Long productId, String variantId, VariantCreateRequest req) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm"));

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy biến thể"));

        if (!variant.getProduct().getId().equals(productId)) {
            throw AppException.badRequest("Biến thể không thuộc sản phẩm này");
        }

        // If color/size changed, check uniqueness
        boolean colorSizeChanged = !normalize(variant.getColor()).equals(normalize(req.getColor()))
                || !normalize(variant.getSize()).equals(normalize(req.getSize()));
        if (colorSizeChanged) {
            boolean exists = product.getVariants().stream()
                    .filter(v -> !v.getId().equals(variantId))
                    .anyMatch(v -> normalize(v.getColor()).equals(normalize(req.getColor()))
                            && normalize(v.getSize()).equals(normalize(req.getSize())));
            if (exists) {
                throw AppException
                        .badRequest("Biến thể với color=" + req.getColor() + ", size=" + req.getSize() + " đã tồn tại");
            }
        }

        variant.setColor(req.getColor());
        variant.setSize(req.getSize());
        variant.setStockQty(req.getStockQty() != null ? req.getStockQty() : 0);
        variant.setSalePrice(req.getSalePrice());
        variant.setImportPrice(req.getImportPrice());
        variant.setSku(generateSku(product, req.getColor(), req.getSize()));
        productVariantRepository.save(variant);

        return VariantDto.builder()
                .id(variant.getId())
                .color(variant.getColor())
                .size(variant.getSize())
                .sku(variant.getSku())
                .stockQty(variant.getStockQty())
                .salePrice(variant.getSalePrice() != null ? variant.getSalePrice() : product.getBasePrice())
                .importPrice(variant.getImportPrice())
                .build();
    }

    @Override
    public void deleteVariant(Long productId, String variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy biến thể"));

        if (!variant.getProduct().getId().equals(productId)) {
            throw AppException.badRequest("Biến thể không thuộc sản phẩm này");
        }

        if (orderItemRepository.existsByVariantId(variantId)) {
            throw AppException.badRequest("Không thể xóa biến thể vì đã có đơn hàng liên kết");
        }

        productVariantRepository.delete(variant);
    }

    @Override
    public void updateVariantStock(String variantId, Integer stockQty) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy biến thể"));
        variant.setStockQty(stockQty != null ? stockQty : 0);
        productVariantRepository.save(variant);
    }

    // ─── Helper methods ──────────────────────────────────

    private void validateVariantUniqueness(List<VariantCreateRequest> variants) {
        Set<String> seen = new HashSet<>();
        for (VariantCreateRequest v : variants) {
            String key = normalize(v.getColor()) + "_" + normalize(v.getSize());
            if (!seen.add(key)) {
                throw AppException.badRequest(
                        "Trùng biến thể: color=" + v.getColor() + ", size=" + v.getSize());
            }
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private ProductFilterRequest prepareSearchFilter(ProductFilterRequest filter, boolean includeHidden) {
        if (includeHidden) {
            return filter;
        }

        List<Long> categoryIds = new ArrayList<>();
        if (filter.getCategoryIds() != null) {
            categoryIds.addAll(filter.getCategoryIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList());
        }
        if (filter.getCategoryId() != null) {
            collectCategoryIds(filter.getCategoryId(), categoryIds);
        }

        ProductFilterRequest.ProductFilterRequestBuilder builder = ProductFilterRequest.builder()
                .keyword(filter.getKeyword())
                .categoryId(filter.getCategoryId())
                .categoryIds(categoryIds.isEmpty() ? null : categoryIds.stream().distinct().toList())
                .minPrice(filter.getMinPrice())
                .maxPrice(filter.getMaxPrice())
                .color(filter.getColor())
                .colors(filter.getColors())
                .size(filter.getSize())
                .sizes(filter.getSizes())
                .inStock(filter.getInStock())
                .status(filter.getStatus())
                .sortBy(filter.getSortBy())
                .page(filter.getPage())
                .pageSize(filter.getPageSize());

        return builder.build();
    }

    private void collectCategoryIds(Long categoryId, List<Long> bucket) {
        if (categoryId == null || bucket.contains(categoryId)) {
            return;
        }

        bucket.add(categoryId);
        for (Category child : categoryRepository.findByParentId(categoryId)) {
            collectCategoryIds(child.getId(), bucket);
        }
    }

    private Sort buildSort(String sortBy) {
        if (sortBy == null)
            return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sortBy) {
            case "name" -> Sort.by(Sort.Direction.ASC, "name");
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "basePrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "basePrice");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private ProductSummaryDto toSummaryDto(Product product) {
        return ProductSummaryDto.builder()
                .id(product.getId())
                .name(product.getName())
                .thumbnailUrl(product.getThumbnailUrl())
                .basePrice(product.getBasePrice())
                .status(product.getStatus())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .build();
    }

    private ProductDetailDto toDetailDto(Product product) {
        // Category DTO (không lấy children để tránh N+1)
        CategoryDto categoryDto = null;
        if (product.getCategory() != null) {
            Category cat = product.getCategory();
            categoryDto = CategoryDto.builder()
                    .id(cat.getId())
                    .name(cat.getName())
                    .parentId(cat.getParent() != null ? cat.getParent().getId() : null)
                    .build();
        }

        // Variants
        List<VariantDto> variantDtos = product.getVariants() == null
                ? Collections.emptyList()
                : product.getVariants().stream()
                        .map(v -> VariantDto.builder()
                                .id(v.getId())
                                .color(v.getColor())
                                .size(v.getSize())
                                .sku(v.getSku())
                                .stockQty(v.getStockQty())
                                .salePrice(v.getSalePrice() != null ? v.getSalePrice() : product.getBasePrice())
                                .importPrice(v.getImportPrice())
                                .build())
                        .collect(Collectors.toList());

        // Image URLs (sorted)
        List<String> imageUrls = product.getImages() == null
                ? Collections.emptyList()
                : product.getImages().stream()
                        .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                        .map(ProductImage::getImageUrl)
                        .collect(Collectors.toList());

        return ProductDetailDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .thumbnailUrl(product.getThumbnailUrl())
                .basePrice(product.getBasePrice())
                .status(product.getStatus())
                .category(categoryDto)
                .variants(variantDtos)
                .imageUrls(imageUrls)
                .build();
    }

    private String generateSku(Product product, String color, String size) {
        String categoryPart = normalizeForSku(product.getCategory().getName());
        String productPart = "P" + product.getId();
        String colorPart = normalizeForSku(color);
        String sizePart = normalizeForSku(size);
        return String.format("%s-%s-%s-%s", categoryPart, productPart, colorPart, sizePart);
    }

    private String normalizeForSku(String input) {
        if (input == null || input.isBlank())
            return "NA";
        // Loại bỏ dấu tiếng Việt cơ bản (nếu có thư viện thì tốt hơn, ở đây dùng regex
        // đơn giản)
        String normalized = input.trim().toUpperCase()
                .replace(" ", "")
                .replaceAll("[^A-Z0-9]", "");
        return normalized.isEmpty() ? "NA" : normalized;
    }

    private Sort buildVariantSort(String sortBy) {
        if (sortBy == null)
            return Sort.by(Sort.Direction.DESC, "sku"); // Default sort
        return switch (sortBy) {
            case "color" -> Sort.by(Sort.Direction.ASC, "color");
            case "size" -> Sort.by(Sort.Direction.ASC, "size");
            case "stock_asc" -> Sort.by(Sort.Direction.ASC, "stockQty");
            case "stock_desc" -> Sort.by(Sort.Direction.DESC, "stockQty");
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "salePrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "salePrice");
            default -> Sort.by(Sort.Direction.DESC, "sku");
        };
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getVariantsByProductId(Long productId, VariantFilterRequest filter) {
        // Validate product exists
        Product product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy sản phẩm với id: " + productId));

        Sort sort = buildVariantSort(filter.getSortBy());
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getPageSize(), sort);

        Page<ProductVariant> variantPage = productVariantRepository.findAll(
                com.clothingstore.clothing_store_be.specification.ProductVariantSpecification.buildFilter(productId, filter),
                pageable);

        List<VariantDto> variantDtos = variantPage.getContent().stream()
                .map(v -> VariantDto.builder()
                        .id(v.getId())
                        .color(v.getColor())
                        .size(v.getSize())
                        .sku(v.getSku())
                        .stockQty(v.getStockQty())
                        .salePrice(v.getSalePrice() != null ? v.getSalePrice() : product.getBasePrice())
                        .importPrice(v.getImportPrice())
                        .build())
                .collect(Collectors.toList());

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta(
                variantPage.getNumber(),
                variantPage.getSize(),
                variantPage.getTotalPages(),
                variantPage.getTotalElements());

        return new ResultPaginationDTO(meta, variantDtos);
    }
}
