package com.example.silkmall.controller;

import com.example.silkmall.dto.CreateProductReviewDTO;
import com.example.silkmall.dto.ProductReviewDTO;
import com.example.silkmall.entity.ProductReview;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.service.ProductReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PreAuthorize("hasAnyRole('CONSUMER', 'ADMIN')")
    public ResponseEntity<ProductReviewDTO> createReview(@PathVariable Long orderItemId,
                                                         @RequestBody CreateProductReviewDTO request,
                                                         @AuthenticationPrincipal CustomUserDetails currentUser) {
        ProductReview review = new ProductReview();
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        ProductReview saved = productReviewService.createReview(orderItemId, review, currentUser);
        return created(toDto(saved));
    }

    @PostMapping("/products/{productId}")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ProductReviewDTO> createDirectReview(@PathVariable Long productId,
                                                               @RequestBody CreateProductReviewDTO request,
                                                               @AuthenticationPrincipal CustomUserDetails currentUser) {
        ProductReview review = new ProductReview();
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        ProductReview saved = productReviewService.createDirectReview(productId, review, currentUser);
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

    @PutMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('CONSUMER', 'SUPPLIER', 'ADMIN')")
    public ResponseEntity<ProductReviewDTO> updateReview(@PathVariable Long reviewId,
                                                         @RequestBody CreateProductReviewDTO request,
                                                         @AuthenticationPrincipal CustomUserDetails currentUser) {
        ProductReview updated = productReviewService.updateReview(reviewId, request.getRating(), request.getComment(), currentUser);
        return success(toDto(updated));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasAnyRole('CONSUMER', 'SUPPLIER', 'ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId,
                                             @AuthenticationPrincipal CustomUserDetails currentUser) {
        productReviewService.deleteReview(reviewId, currentUser);
        return success();
    }

    private ProductReviewDTO toDto(ProductReview review) {
        return ProductReviewDTO.builder()
                .id(review.getId())
                .orderId(review.getOrder() != null ? review.getOrder().getId() : null)
                .orderItemId(review.getOrderItem() != null ? review.getOrderItem().getId() : null)
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .consumerId(review.getConsumer() != null ? review.getConsumer().getId() : null)
                .consumerName(review.getConsumer() != null ? review.getConsumer().getUsername() : null)
                .authorId(review.getAuthorId())
                .authorName(review.getAuthorName())
                .authorRole(review.getAuthorRole())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
