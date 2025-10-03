package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Category;
import com.example.silkmall.repository.CategoryRepository;
import com.example.silkmall.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CategoryServiceImpl extends BaseServiceImpl<Category, Long> implements CategoryService {
    private final CategoryRepository categoryRepository;
    
    @Autowired
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        super(categoryRepository);
        this.categoryRepository = categoryRepository;
    }
    
    @Override
    public List<Category> findEnabledCategories() {
        return categoryRepository.findByEnabledTrue();
    }
    
    @Override
    public List<Category> findByParentId(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }
    
    @Override
    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }

    @Override
    public boolean existsByNameExcludingId(String name, Long id) {
        return categoryRepository.existsByNameAndIdNot(name, id);
    }

    @Override
    public void enableCategory(Long id) {
        Category category = findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        category.setEnabled(true);
        categoryRepository.save(category);
    }
    
    @Override
    public void disableCategory(Long id) {
        Category category = findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        
        category.setEnabled(false);
        categoryRepository.save(category);
    }
    
    @Override
    public List<Category> findRootCategories() {
        return findByParentId(null);
    }
}