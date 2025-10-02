package com.example.silkmall.repository;

import com.example.silkmall.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByStatus(String status, Pageable pageable);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Product> findBySupplierId(Long supplierId, Pageable pageable);
    List<Product> findTop10ByOrderBySalesDesc();
    Page<Product> findByNameContaining(String keyword, Pageable pageable);
}