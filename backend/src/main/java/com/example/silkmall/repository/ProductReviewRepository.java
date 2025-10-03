package com.example.silkmall.repository;

import com.example.silkmall.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    boolean existsByOrderItemId(Long orderItemId);
    List<ProductReview> findByProductId(Long productId);
    List<ProductReview> findByOrderId(Long orderId);
    boolean existsByOrderItemIdAndConsumer_Id(Long orderItemId, Long consumerId);
}
