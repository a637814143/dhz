package com.example.silkmall.controller;

import com.example.silkmall.dto.CreateProductReviewDTO;
import com.example.silkmall.dto.ProductReviewDTO;
import com.example.silkmall.entity.ProductReview;
import com.example.silkmall.service.ProductReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
public class ProductReviewController extends BaseController {
    private final ProductReviewService productReviewService;

    @Autowired
    public ProductReviewController(ProductReviewService productReviewService) {
        this.productReviewService = productReviewService;
    }

    @PostMapping("/order-items/{orderItemId}")
    public ResponseEntity<ProductReviewDTO> createReview(@PathVariable Long orderItemId,
                                                          @RequestBody CreateProductReviewDTO request) {
        ProductReview review = new ProductReview();
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        ProductReview saved = productReviewService.createReview(orderItemId, review);
        return created(toDto(saved));
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<List<ProductReviewDTO>> getByProduct(@PathVariable Long productId) {
        List<ProductReviewDTO> reviews = productReviewService.findByProductId(productId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return success(reviews);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<List<ProductReviewDTO>> getByOrder(@PathVariable Long orderId) {
        List<ProductReviewDTO> reviews = productReviewService.findByOrderId(orderId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return success(reviews);
    }

    private ProductReviewDTO toDto(ProductReview review) {
        return ProductReviewDTO.builder()
                .id(review.getId())
                .orderId(review.getOrder().getId())
                .orderItemId(review.getOrderItem().getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .consumerId(review.getConsumer().getId())
                .consumerName(review.getConsumer().getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
