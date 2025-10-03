package com.example.silkmall.controller;

import com.example.silkmall.dto.CategoryDTO;
import com.example.silkmall.dto.CategoryOptionDTO;
import com.example.silkmall.entity.Category;
import com.example.silkmall.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
public class CategoryController extends BaseController {
    private final CategoryService categoryService;
    
    @Autowired
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        Optional<Category> category = categoryService.findById(id);
        if (category.isPresent()) {
            return success(category.get());
        } else {
            return notFound("分类不存在");
        }
    }
    
    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CategoryDTO categoryDTO) {
        String name = categoryDTO.getName() != null ? categoryDTO.getName().trim() : "";
        if (name.isEmpty()) {
            return badRequest("分类名称不能为空");
        }

        if (categoryService.existsByName(name)) {
            return badRequest("分类名称已存在");
        }

        // 创建Category实体对象
        Category category = new Category();
        category.setName(name);
        category.setDescription(categoryDTO.getDescription());
        category.setSortOrder(categoryDTO.getSortOrder() != null ? categoryDTO.getSortOrder() : 0);
        category.setIcon(categoryDTO.getIcon());
        category.setEnabled(categoryDTO.getEnabled() != null ? categoryDTO.getEnabled() : true);

        // 设置父分类
        if (categoryDTO.getParentId() != null) {
            Category parent = new Category();
            parent.setId(categoryDTO.getParentId());
            category.setParent(parent);
        }
        
        return created(categoryService.save(category));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryDTO categoryDTO) {
        // 检查分类是否存在
        Category existingCategory = categoryService.findById(id)
                .orElseThrow(() -> new RuntimeException("分类不存在"));

        String name = categoryDTO.getName() != null ? categoryDTO.getName().trim() : "";
        if (name.isEmpty()) {
            return badRequest("分类名称不能为空");
        }

        if (categoryService.existsByNameExcludingId(name, id)) {
            return badRequest("分类名称已存在");
        }

        // 更新分类信息
        existingCategory.setName(name);
        existingCategory.setDescription(categoryDTO.getDescription());
        if (categoryDTO.getSortOrder() != null) {
            existingCategory.setSortOrder(categoryDTO.getSortOrder());
        }
        existingCategory.setIcon(categoryDTO.getIcon());
        existingCategory.setEnabled(categoryDTO.getEnabled() != null ? categoryDTO.getEnabled() : true);
        
        // 设置父分类
        if (categoryDTO.getParentId() != null) {
            Category parent = new Category();
            parent.setId(categoryDTO.getParentId());
            existingCategory.setParent(parent);
        } else {
            existingCategory.setParent(null);
        }
        
        return success(categoryService.save(existingCategory));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteById(id);
        return success();
    }
    
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return success(categoryService.findAll());
    }

    @GetMapping("/enabled")
    public ResponseEntity<List<Category>> getEnabledCategories() {
        return success(categoryService.findEnabledCategories());
    }

    @GetMapping("/options")
    public ResponseEntity<List<CategoryOptionDTO>> getCategoryOptions() {
        return success(categoryService.findAllOptions());
    }
    
    @GetMapping("/parent/{parentId}")
    public ResponseEntity<List<Category>> getCategoriesByParentId(@PathVariable Long parentId) {
        return success(categoryService.findByParentId(parentId));
    }
    
    @GetMapping("/root")
    public ResponseEntity<List<Category>> getRootCategories() {
        return success(categoryService.findRootCategories());
    }
    
    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enableCategory(@PathVariable Long id) {
        categoryService.enableCategory(id);
        return success();
    }
    
    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disableCategory(@PathVariable Long id) {
        categoryService.disableCategory(id);
        return success();
    }
}