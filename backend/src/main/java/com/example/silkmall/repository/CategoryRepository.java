package com.example.silkmall.repository;

import com.example.silkmall.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByEnabledTrue();
    List<Category> findByParentId(Long parentId);
    boolean existsByName(String name);
}