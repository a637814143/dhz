package com.example.silkmall.repository;

import com.example.silkmall.dto.CategoryOptionDTO;
import com.example.silkmall.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByEnabledTrue();
    List<Category> findByParentId(Long parentId);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);

    @Query("select new com.example.silkmall.dto.CategoryOptionDTO(c.id, c.name) " +
            "from Category c order by coalesce(c.sortOrder, 0), lower(coalesce(c.name, '')), c.id")
    List<CategoryOptionDTO> findAllOptions();
}
