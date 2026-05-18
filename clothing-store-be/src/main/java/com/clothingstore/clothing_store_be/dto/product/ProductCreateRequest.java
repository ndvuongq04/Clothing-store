package com.clothingstore.clothing_store_be.dto.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ProductCreateRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    private String description;

    private String thumbnailUrl;

    @NotNull(message = "Giá cơ bản không được để trống")
    @Positive(message = "Giá cơ bản phải lớn hơn 0")
    private BigDecimal basePrice;

    @Builder.Default
    private Integer status = 1;

    @Valid
    private List<VariantCreateRequest> variants;

    @Size(max = 10, message = "Tối đa 10 ảnh sản phẩm")
    private List<String> imageUrls;

    private Integer excelRow;
}
