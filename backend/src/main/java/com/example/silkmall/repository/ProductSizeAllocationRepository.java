package com.example.silkmall.repository;

import com.example.silkmall.entity.ProductSizeAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSizeAllocationRepository extends JpaRepository<ProductSizeAllocation, Long> {
    List<ProductSizeAllocation> findByProductId(Long productId);
    void deleteByProductId(Long productId);
    java.util.Optional<ProductSizeAllocation> findByProductIdAndSizeLabel(Long productId, String sizeLabel);
}
