package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, String>, JpaSpecificationExecutor<ProductVariant> {

    List<ProductVariant> findByProductId(String productId);

    Page<ProductVariant> findByProductId(Long productId, Pageable pageable);

    Optional<ProductVariant> findByProductIdAndColorAndSize(String productId, String color, String size);

    long countByStockQtyLessThan(Integer stockQty);
}
