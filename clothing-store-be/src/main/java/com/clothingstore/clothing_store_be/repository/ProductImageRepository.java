package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, String> {

    List<ProductImage> findByProductIdOrderBySortOrder(String productId);
}
