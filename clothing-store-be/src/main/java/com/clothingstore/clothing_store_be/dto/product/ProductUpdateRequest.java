package com.clothingstore.clothing_store_be.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductUpdateRequest {

    private String name;

    private Long categoryId;

    private String description;

    private String thumbnailUrl;

    @Positive(message = "Giá cơ bản phải lớn hơn 0")
    private BigDecimal basePrice;

    private Integer status;

    @Valid
    private List<VariantCreateRequest> variants;

    @Size(max = 10, message = "Tối đa 10 ảnh sản phẩm")
    private List<String> imageUrls;
}
