package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.dto.review.CreateReviewRequest;
import com.clothingstore.clothing_store_be.dto.review.PendingReviewItemDto;
import com.clothingstore.clothing_store_be.dto.review.ReviewDto;
import com.clothingstore.clothing_store_be.dto.review.UpdateReviewRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReviewService {

    ReviewDto createReview(Long userId, CreateReviewRequest req, List<MultipartFile> images);

    ReviewDto updateReview(Long userId, Long reviewId, UpdateReviewRequest req, List<MultipartFile> newImages, List<String> removeImageUrls);

    void deleteReview(Long userId, Long reviewId);

    ReviewDto toggleLike(Long userId, Long reviewId);

    ResultPaginationDTO getProductReviews(Long productId, Long currentUserId, int page, int pageSize);

    ResultPaginationDTO getMyReviews(Long userId, int page, int pageSize);

    List<PendingReviewItemDto> getMyPendingReviewItems(Long userId);
}
