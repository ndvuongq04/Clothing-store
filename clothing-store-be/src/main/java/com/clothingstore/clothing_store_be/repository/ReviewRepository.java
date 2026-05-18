package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderItemId(Long orderItemId);

    Page<Review> findByProductId(Long productId, Pageable pageable);

    Optional<Review> findByIdAndUserUserId(Long id, Long userId);

    Page<Review> findByUserUserId(Long userId, Pageable pageable);
}
