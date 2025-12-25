package com.example.silkmall.service;

import com.example.silkmall.dto.ProductOverviewDTO;
import com.example.silkmall.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;

public interface ProductService extends BaseService<Product, Long> {
    Page<Product> findByStatus(String status, Pageable pageable);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Product> findBySupplierId(Long supplierId, Pageable pageable);
    List<Product> findTop10ByOrderBySalesDesc();
    List<Product> findTop8ByStatusOrderByCreatedAtDesc(String status);
    List<Product> findTop8ByStatusOrderBySalesDesc(String status);
    List<Product> findTop8ByStatusOrderByPriceAsc(String status);
    Page<Product> search(String keyword, Pageable pageable);
    Page<Product> advancedSearch(String keyword,
                                 Long categoryId,
                                 Long supplierId,
                                 BigDecimal minPrice,
                                 BigDecimal maxPrice,
                                 String status,
                                 Pageable pageable);
    void updateStock(Long id, Integer quantity);
    void increaseSales(Long id, Integer quantity);
    void putProductOnSale(Long id);
    void takeProductOffSale(Long id);
    ProductOverviewDTO getProductOverview();
    Product withSizeAllocations(Product product);
}
