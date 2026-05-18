package com.clothingstore.clothing_store_be.dto.review;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewDto {
    private Long reviewId;
    private Long userId;
    private String userFullName;
    private Long productId;
    private Long orderItemId;
    private Integer starRating;
    private String content;
    private Integer likeCount;
    private boolean likedByMe;        // true nếu user hiện tại đã like
    private List<String> imageUrls;   // danh sách url ảnh (UC-27 sau)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
