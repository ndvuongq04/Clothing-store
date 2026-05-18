package com.clothingstore.clothing_store_be.dto.product;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDto {
    private Long id;
    private String name;
    private Long parentId;
    private List<CategoryDto> children;
}
