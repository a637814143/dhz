package com.example.silkmall.dto;

import java.util.Date;

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

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getConsumerId() {
        return consumerId;
    }

    public void setConsumerId(Long consumerId) {
        this.consumerId = consumerId;
    }

    public String getConsumerName() {
        return consumerName;
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public static final class Builder {
        private final ProductReviewDTO instance;

        private Builder() {
            this.instance = new ProductReviewDTO();
        }

        public Builder id(Long id) {
            instance.setId(id);
            return this;
        }

        public Builder orderId(Long orderId) {
            instance.setOrderId(orderId);
            return this;
        }

        public Builder orderItemId(Long orderItemId) {
            instance.setOrderItemId(orderItemId);
            return this;
        }

        public Builder productId(Long productId) {
            instance.setProductId(productId);
            return this;
        }

        public Builder productName(String productName) {
            instance.setProductName(productName);
            return this;
        }

        public Builder consumerId(Long consumerId) {
            instance.setConsumerId(consumerId);
            return this;
        }

        public Builder consumerName(String consumerName) {
            instance.setConsumerName(consumerName);
            return this;
        }

        public Builder rating(Integer rating) {
            instance.setRating(rating);
            return this;
        }

        public Builder comment(String comment) {
            instance.setComment(comment);
            return this;
        }

        public Builder createdAt(Date createdAt) {
            instance.setCreatedAt(createdAt);
            return this;
        }

        public ProductReviewDTO build() {
            return instance;
        }
    }
}
