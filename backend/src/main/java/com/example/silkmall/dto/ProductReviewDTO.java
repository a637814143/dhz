package com.example.silkmall.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class ProductReviewDTO {
    private Long id;
    private Long orderId;
    private Long orderItemId;
    private Long productId;
    private String productName;
    private Long consumerId;
    private String consumerName;
    private Integer rating;
    private String comment;
    private Date createdAt;
}
