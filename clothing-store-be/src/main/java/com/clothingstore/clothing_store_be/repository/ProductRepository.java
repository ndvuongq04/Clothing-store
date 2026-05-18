package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // Tìm sản phẩm chưa bị xoá mềm
    Optional<Product> findByIdAndDeletedAtIsNull(Long id);

    // Phân trang sản phẩm chưa bị xoá mềm
    Page<Product> findAllByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT p.id, p.name, p.thumbnailUrl, c.name, SUM(v.stockQty) " +
            "FROM ProductVariant v " +
            "JOIN v.product p " +
            "JOIN p.category c " +
            "WHERE p.deletedAt IS NULL AND v.stockQty > 0 " +
            "GROUP BY p.id, p.name, p.thumbnailUrl, c.name " +
            "ORDER BY SUM(v.stockQty) DESC")
    Page<Object[]> findSlowMovingProducts(Pageable pageable);

    boolean existsByNameAndDeletedAtIsNull(String name);
}
