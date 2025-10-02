package com.example.silkmall.service;

import com.example.silkmall.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ProductService extends BaseService<Product, Long> {
    Page<Product> findByStatus(String status, Pageable pageable);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Product> findBySupplierId(Long supplierId, Pageable pageable);
    List<Product> findTop10ByOrderBySalesDesc();
    Page<Product> search(String keyword, Pageable pageable);
    void updateStock(Long id, Integer quantity);
    void increaseSales(Long id, Integer quantity);
    void putProductOnSale(Long id);
    void takeProductOffSale(Long id);
}