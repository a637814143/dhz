package com.example.silkmall.dto;

import java.util.Date;

public class ReturnRequestDTO {
    private Long id;
    private Long orderId;
    private Long orderItemId;
    private Long productId;
    private String productName;
    private Long consumerId;
    private String consumerName;
    private String status;
    private String reason;
    private String resolution;
    private Date requestedAt;
    private Date processedAt;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public Date getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Date requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Date getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Date processedAt) {
        this.processedAt = processedAt;
    }

    public static final class Builder {
        private final ReturnRequestDTO instance;

        private Builder() {
            this.instance = new ReturnRequestDTO();
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

        public Builder status(String status) {
            instance.setStatus(status);
            return this;
        }

        public Builder reason(String reason) {
            instance.setReason(reason);
            return this;
        }

        public Builder resolution(String resolution) {
            instance.setResolution(resolution);
            return this;
        }

        public Builder requestedAt(Date requestedAt) {
            instance.setRequestedAt(requestedAt);
            return this;
        }

        public Builder processedAt(Date processedAt) {
            instance.setProcessedAt(processedAt);
            return this;
        }

        public ReturnRequestDTO build() {
            return instance;
        }
    }
}
