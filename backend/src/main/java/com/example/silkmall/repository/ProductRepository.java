package com.example.silkmall.repository;

import com.example.silkmall.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Page<Product> findByStatus(String status, Pageable pageable);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Product> findBySupplierId(Long supplierId, Pageable pageable);
    List<Product> findTop10ByOrderBySalesDesc();
    List<Product> findTop8ByStatusOrderByCreatedAtDesc(String status);
    List<Product> findTop8ByStatusOrderBySalesDesc(String status);
    List<Product> findTop8ByStatusOrderByPriceAsc(String status);
    Page<Product> findByNameContaining(String keyword, Pageable pageable);
    long countByStatus(String status);
    long countByStockLessThanEqual(Integer stock);

    @Query("select coalesce(sum(p.stock), 0) from Product p")
    Long sumStock();

    @Query("select coalesce(sum(p.sales), 0) from Product p")
    Long sumSales();
}