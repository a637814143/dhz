package com.example.silkmall.service.impl;

import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.ProductReview;
import com.example.silkmall.repository.OrderItemRepository;
import com.example.silkmall.repository.ProductReviewRepository;
import com.example.silkmall.service.ProductReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductReviewServiceImpl extends BaseServiceImpl<ProductReview, Long> implements ProductReviewService {
    private final ProductReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public ProductReviewServiceImpl(ProductReviewRepository reviewRepository,
                                    OrderItemRepository orderItemRepository) {
        super(reviewRepository);
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public ProductReview createReview(Long orderItemId, ProductReview review) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("订单项不存在"));

        if (!"DELIVERED".equals(orderItem.getOrder().getStatus())) {
            throw new RuntimeException("只有已收货的订单才能评价");
        }

        if (reviewRepository.existsByOrderItemId(orderItemId)) {
            throw new RuntimeException("该商品已评价过");
        }

        Integer rating = review.getRating();
        if (rating == null || rating < 1 || rating > 5) {
            throw new RuntimeException("评分必须在1到5之间");
        }

        review.setOrder(orderItem.getOrder());
        review.setOrderItem(orderItem);
        review.setProduct(orderItem.getProduct());
        review.setConsumer(orderItem.getOrder().getConsumer());

        return reviewRepository.save(review);
    }

    @Override
    public List<ProductReview> findByProductId(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    @Override
    public List<ProductReview> findByOrderId(Long orderId) {
        return reviewRepository.findByOrderId(orderId);
    }
}
