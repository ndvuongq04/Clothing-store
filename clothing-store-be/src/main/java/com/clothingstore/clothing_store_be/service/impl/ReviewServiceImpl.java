package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.dto.review.CreateReviewRequest;
import com.clothingstore.clothing_store_be.dto.review.PendingReviewItemDto;
import com.clothingstore.clothing_store_be.dto.review.ReviewDto;
import com.clothingstore.clothing_store_be.dto.review.UpdateReviewRequest;
import com.clothingstore.clothing_store_be.entity.*;
import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.repository.*;
import com.clothingstore.clothing_store_be.service.FileStorageService;
import com.clothingstore.clothing_store_be.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_IMAGES = 5;
    private static final String IMAGE_SUBFOLDER = "reviews";

    private final ReviewRepository reviewRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    // ─── UC-26 + UC-27: Tạo đánh giá + Upload ảnh ────────

    @Override
    public ReviewDto createReview(Long userId, CreateReviewRequest req, List<MultipartFile> images) {
        // Xác nhận đơn hàng đã hoàn thành và item thuộc về user
        OrderItem orderItem = orderItemRepository
                .findCompletedItemByIdAndUser(req.getOrderItemId(), userId)
                .orElseThrow(() -> AppException.badRequest(
                        "Bạn chỉ có thể đánh giá sản phẩm trong đơn hàng đã hoàn thành"));

        // Mỗi order item chỉ được đánh giá một lần
        if (reviewRepository.existsByOrderItemId(req.getOrderItemId())) {
            throw AppException.conflict("Bạn đã đánh giá sản phẩm này rồi");
        }

        // Validate số lượng ảnh
        if (images != null && images.size() > MAX_IMAGES) {
            throw AppException.badRequest("Tối đa " + MAX_IMAGES + " ảnh cho mỗi đánh giá");
        }

        Product product = orderItem.getVariant().getProduct();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Người dùng không tồn tại"));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .orderItem(orderItem)
                .starRating(req.getStarRating())
                .content(req.getContent())
                .build();

        review = reviewRepository.save(review);

        // Upload và lưu ảnh
        if (images != null && !images.isEmpty()) {
            List<String> uploadedUrls = fileStorageService.uploadFiles(IMAGE_SUBFOLDER, images);
            for (String url : uploadedUrls) {
                ReviewImage img = ReviewImage.builder()
                        .review(review)
                        .imageUrl(url)
                        .build();
                review.getImages().add(img);
            }
            review = reviewRepository.save(review);
        }

        return toDto(review, false);
    }

    // ─── UC-28: Chỉnh sửa đánh giá ───────────────────────

    @Override
    public ReviewDto updateReview(Long userId, Long reviewId, UpdateReviewRequest req,
            List<MultipartFile> newImages, List<String> removeImageUrls) {
        Review review = reviewRepository.findByIdAndUserUserId(reviewId, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đánh giá của bạn"));

        review.setStarRating(req.getStarRating());
        review.setContent(req.getContent());

        // Xóa các ảnh được yêu cầu xóa
        if (removeImageUrls != null && !removeImageUrls.isEmpty()) {
            review.getImages().removeIf(img -> {
                if (removeImageUrls.contains(img.getImageUrl())) {
                    fileStorageService.deleteFile(img.getImageUrl());
                    return true;
                }
                return false;
            });
        }

        // Validate tổng số ảnh sau khi thêm mới
        int currentCount = review.getImages().size();
        int addCount = (newImages != null) ? newImages.size() : 0;
        if (currentCount + addCount > MAX_IMAGES) {
            throw AppException.badRequest("Tổng số ảnh không được vượt quá " + MAX_IMAGES);
        }

        // Upload ảnh mới
        if (newImages != null && !newImages.isEmpty()) {
            List<String> uploadedUrls = fileStorageService.uploadFiles(IMAGE_SUBFOLDER, newImages);
            for (String url : uploadedUrls) {
                ReviewImage img = ReviewImage.builder()
                        .review(review)
                        .imageUrl(url)
                        .build();
                review.getImages().add(img);
            }
        }

        boolean likedByMe = reviewLikeRepository.existsByReviewIdAndUserUserId(reviewId, userId);
        return toDto(reviewRepository.save(review), likedByMe);
    }

    // ─── UC-28: Xóa đánh giá ─────────────────────────────

    @Override
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findByIdAndUserUserId(reviewId, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đánh giá của bạn"));

        // Xóa tất cả ảnh trên disk
        for (ReviewImage img : review.getImages()) {
            fileStorageService.deleteFile(img.getImageUrl());
        }

        reviewRepository.delete(review);
    }

    // ─── UC-28b: Toggle like ──────────────────────────────

    @Override
    public ReviewDto toggleLike(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đánh giá"));

        if (review.getUser().getUserId().equals(userId)) {
            throw AppException.badRequest("Bạn không thể like đánh giá của chính mình");
        }

        boolean alreadyLiked = reviewLikeRepository.existsByReviewIdAndUserUserId(reviewId, userId);

        if (alreadyLiked) {
            ReviewLike like = reviewLikeRepository
                    .findByReviewIdAndUserUserId(reviewId, userId).get();
            reviewLikeRepository.delete(like);
            review.setLikeCount(review.getLikeCount() - 1);
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> AppException.notFound("Người dùng không tồn tại"));
            reviewLikeRepository.save(ReviewLike.builder()
                    .review(review)
                    .user(user)
                    .build());
            review.setLikeCount(review.getLikeCount() + 1);
        }

        return toDto(reviewRepository.save(review), !alreadyLiked);
    }

    // ─── Danh sách đánh giá theo sản phẩm ────────────────

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getProductReviews(Long productId, Long currentUserId, int page, int pageSize) {
        if (!productRepository.existsById(productId)) {
            throw AppException.notFound("Không tìm thấy sản phẩm");
        }

        Page<Review> result = reviewRepository.findByProductId(
                productId,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta(
                result.getNumber(), result.getSize(),
                result.getTotalPages(), result.getTotalElements());

        List<ReviewDto> content = result.getContent().stream().map(r -> {
            boolean likedByMe = currentUserId != null &&
                    reviewLikeRepository.existsByReviewIdAndUserUserId(r.getId(), currentUserId);
            return toDto(r, likedByMe);
        }).toList();

        return new ResultPaginationDTO(meta, content);
    }

    // ─── Đánh giá của tôi ─────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getMyReviews(Long userId, int page, int pageSize) {
        Page<Review> result = reviewRepository.findByUserUserId(
                userId,
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta(
                result.getNumber(), result.getSize(),
                result.getTotalPages(), result.getTotalElements());

        List<ReviewDto> content = result.getContent().stream()
                .map(r -> toDto(r, false))
                .toList();

        return new ResultPaginationDTO(meta, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingReviewItemDto> getMyPendingReviewItems(Long userId) {
        return orderItemRepository.findCompletedItemsWithoutReviewByUserId(userId).stream()
                .map(this::toPendingReviewItemDto)
                .toList();
    }

    // ─── Helper ───────────────────────────────────────────

    private ReviewDto toDto(Review r, boolean likedByMe) {
        List<String> imageUrls = r.getImages() == null ? List.of()
                : r.getImages().stream().map(ReviewImage::getImageUrl).toList();

        return ReviewDto.builder()
                .reviewId(r.getId())
                .userId(r.getUser().getUserId())
                .userFullName(r.getUser().getFullName())
                .productId(r.getProduct().getId())
                .orderItemId(r.getOrderItem().getId())
                .starRating(r.getStarRating())
                .content(r.getContent())
                .likeCount(r.getLikeCount())
                .likedByMe(likedByMe)
                .imageUrls(imageUrls)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private PendingReviewItemDto toPendingReviewItemDto(OrderItem orderItem) {
        return PendingReviewItemDto.builder()
                .orderItemId(orderItem.getId())
                .orderId(orderItem.getOrder().getId())
                .orderCode(orderItem.getOrder().getOrderCode())
                .productId(orderItem.getVariant().getProduct().getId())
                .variantId(orderItem.getVariant().getId())
                .productName(orderItem.getProductName())
                .color(orderItem.getColor())
                .size(orderItem.getSize())
                .thumbnailUrl(orderItem.getThumbnailUrl())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .lineTotal(orderItem.getLineTotal())
                .completedAt(orderItem.getOrder().getUpdatedAt())
                .build();
    }
}
