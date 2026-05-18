package com.clothingstore.clothing_store_be.dto.product;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResultDto {
    private int totalRows;
    private int success;
    private int failed;
    private List<String> successProducts;
    private List<String> errors;
}
