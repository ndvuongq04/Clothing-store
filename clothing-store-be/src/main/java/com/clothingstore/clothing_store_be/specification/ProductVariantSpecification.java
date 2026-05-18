package com.clothingstore.clothing_store_be.specification;

import com.clothingstore.clothing_store_be.dto.product.VariantFilterRequest;
import com.clothingstore.clothing_store_be.entity.ProductVariant;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductVariantSpecification {

    public static Specification<ProductVariant> buildFilter(Long productId, VariantFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Luôn lọc theo productId
            predicates.add(cb.equal(root.get("product").get("id"), productId));

            // Lọc theo màu sắc
            if (filter.getColor() != null && !filter.getColor().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("color"), filter.getColor().trim()));
            }

            // Lọc theo size
            if (filter.getSize() != null && !filter.getSize().trim().isEmpty()) {
                predicates.add(cb.equal(root.get("size"), filter.getSize().trim()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
