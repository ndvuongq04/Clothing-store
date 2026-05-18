package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.ReviewLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    Optional<ReviewLike> findByReviewIdAndUserUserId(Long reviewId, Long userId);

    boolean existsByReviewIdAndUserUserId(Long reviewId, Long userId);
}
