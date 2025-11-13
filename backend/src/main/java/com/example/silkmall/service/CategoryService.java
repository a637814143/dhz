package com.example.silkmall.service;

import com.example.silkmall.dto.CategoryOptionDTO;
import com.example.silkmall.entity.Category;
import java.util.List;

public interface CategoryService extends BaseService<Category, Long> {
    List<Category> findEnabledCategories();
    List<Category> findByParentId(Long parentId);
    boolean existsByName(String name);
    boolean existsByNameExcludingId(String name, Long id);
    void enableCategory(Long id);
    void disableCategory(Long id);
    List<Category> findRootCategories();
    List<CategoryOptionDTO> findAllOptions();
}