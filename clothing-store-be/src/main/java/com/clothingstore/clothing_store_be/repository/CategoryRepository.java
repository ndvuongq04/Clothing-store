package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Lấy danh mục gốc (không có parent)
    List<Category> findByParentIsNull();

    // Lấy danh mục con theo parentId
    List<Category> findByParentId(Long parentId);
}
