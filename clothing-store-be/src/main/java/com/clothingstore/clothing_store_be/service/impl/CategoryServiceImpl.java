package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.dto.product.CategoryCreateRequest;
import com.clothingstore.clothing_store_be.dto.product.CategoryDto;
import com.clothingstore.clothing_store_be.entity.Category;
import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.repository.CategoryRepository;
import com.clothingstore.clothing_store_be.repository.ProductRepository;
import com.clothingstore.clothing_store_be.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllTree() {
        List<Category> roots = categoryRepository.findByParentIsNull();
        return roots.stream()
                .map(this::buildTree)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy danh mục với id: " + id));
        return buildTree(category);
    }

    @Override
    public CategoryDto create(CategoryCreateRequest req) {
        Category category = new Category();
        category.setName(req.getName());

        if (req.getParentId() != null) {
            Category parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> AppException.notFound(
                            "Không tìm thấy danh mục cha với id: " + req.getParentId()));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        return toDto(saved);
    }

    @Override
    public CategoryDto update(Long id, CategoryCreateRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy danh mục với id: " + id));

        category.setName(req.getName());

        if (req.getParentId() != null) {
            // Không cho phép danh mục trở thành tổ tiên của chính nó
            if (req.getParentId().equals(id)) {
                throw AppException.badRequest("Danh mục không thể là cha của chính nó");
            }
            // Kiểm tra parentId không phải là con cháu của category hiện tại
            if (isDescendant(id, req.getParentId())) {
                throw AppException.badRequest("Không thể đặt danh mục con làm danh mục cha (vòng lặp)");
            }

            Category parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> AppException.notFound(
                            "Không tìm thấy danh mục cha với id: " + req.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Category saved = categoryRepository.save(category);
        return toDto(saved);
    }

    @Override
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy danh mục với id: " + id));

        // Không xoá nếu còn danh mục con
        List<Category> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw AppException.badRequest("Không thể xoá danh mục vì còn danh mục con");
        }

        // Không xoá nếu còn sản phẩm liên kết
        long productCount = productRepository.count(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("category").get("id"), id),
                        cb.isNull(root.get("deletedAt"))
                )
        );
        if (productCount > 0) {
            throw AppException.badRequest("Không thể xoá danh mục vì còn " + productCount + " sản phẩm liên kết");
        }

        categoryRepository.delete(category);
    }

    // ─── Helper methods ──────────────────────────────────

    /**
     * Kiểm tra possibleDescendantId có phải là con cháu của ancestorId không
     */
    private boolean isDescendant(Long ancestorId, Long possibleDescendantId) {
        List<Category> children = categoryRepository.findByParentId(ancestorId);
        for (Category child : children) {
            if (child.getId().equals(possibleDescendantId)) {
                return true;
            }
            if (isDescendant(child.getId(), possibleDescendantId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Xây dựng cây danh mục đệ quy
     */
    private CategoryDto buildTree(Category category) {
        List<Category> children = categoryRepository.findByParentId(category.getId());
        List<CategoryDto> childDtos = children.isEmpty()
                ? Collections.emptyList()
                : children.stream().map(this::buildTree).collect(Collectors.toList());

        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .children(childDtos)
                .build();
    }

    private CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .children(Collections.emptyList())
                .build();
    }
}
