package com.example.silkmall.service;

import com.example.silkmall.entity.ProductReview;
import com.example.silkmall.security.CustomUserDetails;

import java.util.List;

public interface ProductReviewService extends BaseService<ProductReview, Long> {
    ProductReview createReview(Long orderItemId, ProductReview review, CustomUserDetails author);
    ProductReview createDirectReview(Long productId, ProductReview review, CustomUserDetails author);
    ProductReview updateReview(Long reviewId, Integer rating, String comment, CustomUserDetails author);
    void deleteReview(Long reviewId, CustomUserDetails actor);
    List<ProductReview> findByProductId(Long productId);
    List<ProductReview> findByOrderId(Long orderId);
}
