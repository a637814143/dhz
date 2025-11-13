package com.example.silkmall.service;

import com.example.silkmall.entity.ProductReview;

import java.util.List;

public interface ProductReviewService extends BaseService<ProductReview, Long> {
    ProductReview createReview(Long orderItemId, ProductReview review);
    List<ProductReview> findByProductId(Long productId);
    List<ProductReview> findByOrderId(Long orderId);
}
