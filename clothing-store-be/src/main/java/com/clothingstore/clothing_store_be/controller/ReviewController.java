package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.dto.review.CreateReviewRequest;
import com.clothingstore.clothing_store_be.dto.review.PendingReviewItemDto;
import com.clothingstore.clothing_store_be.dto.review.ReviewDto;
import com.clothingstore.clothing_store_be.dto.review.UpdateReviewRequest;
import com.clothingstore.clothing_store_be.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // ─── UC-26 + UC-27: Tạo đánh giá (có thể kèm ảnh) ─────
    // Content-Type: multipart/form-data
    // Fields: orderItemId, starRating, content (text), images (file[])

    @PostMapping(value = "/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewDto> createReview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("orderItemId") Long orderItemId,
            @RequestParam("starRating") Integer starRating,
            @RequestParam(value = "content", required = false) String content,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        CreateReviewRequest req = new CreateReviewRequest();
        req.setOrderItemId(orderItemId);
        req.setStarRating(starRating);
        req.setContent(content);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(userId(jwt), req, images));
    }

    // ─── UC-28: Chỉnh sửa đánh giá (thêm/xóa ảnh) ─────────

    @PutMapping(value = "/reviews/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewDto> updateReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("reviewId") Long reviewId,
            @RequestParam("starRating") Integer starRating,
            @RequestParam(value = "content", required = false) String content,
            @RequestPart(value = "images", required = false) List<MultipartFile> newImages,
            @RequestParam(value = "removeImageUrls", required = false) List<String> removeImageUrls) {

        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setStarRating(starRating);
        req.setContent(content);

        return ResponseEntity.ok(
                reviewService.updateReview(userId(jwt), reviewId, req, newImages, removeImageUrls));
    }

    // ─── UC-28: Xóa đánh giá ─────────────────────────────────

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("reviewId") Long reviewId) {
        reviewService.deleteReview(userId(jwt), reviewId);
        return ResponseEntity.noContent().build();
    }

    // ─── UC-28b: Like / Unlike đánh giá ──────────────────────

    @PostMapping("/reviews/{reviewId}/like")
    public ResponseEntity<ReviewDto> toggleLike(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("reviewId") Long reviewId) {
        return ResponseEntity.ok(reviewService.toggleLike(userId(jwt), reviewId));
    }

    // ─── Xem đánh giá theo sản phẩm (public) ─────────────────

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ResultPaginationDTO> getProductReviews(
            @PathVariable("productId") Long productId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        Long currentUserId = (jwt != null) ? Long.parseLong(jwt.getSubject()) : null;
        return ResponseEntity.ok(
                reviewService.getProductReviews(productId, currentUserId, page, pageSize));
    }

    // ─── Đánh giá của tôi ─────────────────────────────────────

    @GetMapping("/reviews/me")
    public ResponseEntity<ResultPaginationDTO> getMyReviews(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(reviewService.getMyReviews(userId(jwt), page, pageSize));
    }

    @GetMapping("/reviews/me/pending")
    public ResponseEntity<List<PendingReviewItemDto>> getMyPendingReviewItems(
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(reviewService.getMyPendingReviewItems(userId(jwt)));
    }

    // ─── Helper ───────────────────────────────────────────────

    private Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
