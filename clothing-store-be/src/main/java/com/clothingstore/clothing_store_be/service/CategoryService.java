package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.product.CategoryCreateRequest;
import com.clothingstore.clothing_store_be.dto.product.CategoryDto;

import java.util.List;

public interface CategoryService {

    List<CategoryDto> getAllTree();

    CategoryDto getById(Long id);

    CategoryDto create(CategoryCreateRequest req);

    CategoryDto update(Long id, CategoryCreateRequest req);

    void delete(Long id);
}
