package com.clothingstore.clothing_store_be.specification;

import com.clothingstore.clothing_store_be.dto.product.ProductFilterRequest;
import com.clothingstore.clothing_store_be.entity.Product;
import com.clothingstore.clothing_store_be.entity.ProductVariant;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    private ProductSpecification() {
    }

    public static Specification<Product> buildFilter(ProductFilterRequest filter, boolean includeHidden) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!includeHidden) {
                predicates.add(cb.isNull(root.get("deletedAt")));
                predicates.add(cb.equal(root.get("status"), 1));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String keyword = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), keyword),
                        cb.like(cb.lower(root.get("description")), keyword)));
            }

            if (filter.getCategoryIds() != null && !filter.getCategoryIds().isEmpty()) {
                predicates.add(root.get("category").get("id").in(filter.getCategoryIds()));
            } else if (filter.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), filter.getCategoryId()));
            }

            List<String> colors = normalizeLowerValues(filter.getColors(), filter.getColor());
            List<String> sizes = normalizeUpperValues(filter.getSizes(), filter.getSize());
            boolean filterByVariant = !colors.isEmpty() || !sizes.isEmpty() || Boolean.TRUE.equals(filter.getInStock());

            Join<Product, ProductVariant> variantJoin = null;
            Expression<BigDecimal> priceExpression = root.get("basePrice");

            if (filterByVariant) {
                variantJoin = root.join("variants", JoinType.LEFT);
                priceExpression = cb.<BigDecimal>coalesce(variantJoin.get("salePrice"), root.get("basePrice"));

                if (!colors.isEmpty()) {
                    predicates.add(cb.lower(variantJoin.get("color")).in(colors));
                }
                if (!sizes.isEmpty()) {
                    predicates.add(cb.upper(variantJoin.get("size")).in(sizes));
                }
                if (Boolean.TRUE.equals(filter.getInStock())) {
                    predicates.add(cb.greaterThan(variantJoin.get("stockQty"), 0));
                }

                query.distinct(true);
            }

            if (filter.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(priceExpression, filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(priceExpression, filter.getMaxPrice()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static List<String> normalizeLowerValues(List<String> values, String singleValue) {
        List<String> normalized = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    normalized.add(value.trim().toLowerCase());
                }
            }
        }
        if (singleValue != null && !singleValue.isBlank()) {
            normalized.add(singleValue.trim().toLowerCase());
        }
        return normalized.stream().distinct().toList();
    }

    private static List<String> normalizeUpperValues(List<String> values, String singleValue) {
        List<String> normalized = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    normalized.add(value.trim().toUpperCase());
                }
            }
        }
        if (singleValue != null && !singleValue.isBlank()) {
            normalized.add(singleValue.trim().toUpperCase());
        }
        return normalized.stream().distinct().toList();
    }
}
