package com.example.silkmall.service;

import com.example.silkmall.entity.Category;
import java.util.List;

public interface CategoryService extends BaseService<Category, Long> {
    List<Category> findEnabledCategories();
    List<Category> findByParentId(Long parentId);
    boolean existsByName(String name);
    void enableCategory(Long id);
    void disableCategory(Long id);
    List<Category> findRootCategories();
}