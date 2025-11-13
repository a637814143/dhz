package com.example.silkmall.dto;

import java.math.BigDecimal;
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
    private String adminResolution;
    private Date requestedAt;
    private Date processedAt;
    private Date adminProcessedAt;
    private Boolean afterReceipt;
    private Boolean requiresAdminApproval;
    private String adminStatus;
    private BigDecimal refundAmount;
    private BigDecimal supplierShareAmount;
    private BigDecimal commissionAmount;

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

    public String getAdminResolution() {
        return adminResolution;
    }

    public void setAdminResolution(String adminResolution) {
        this.adminResolution = adminResolution;
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

    public Date getAdminProcessedAt() {
        return adminProcessedAt;
    }

    public void setAdminProcessedAt(Date adminProcessedAt) {
        this.adminProcessedAt = adminProcessedAt;
    }

    public Boolean getAfterReceipt() {
        return afterReceipt;
    }

    public void setAfterReceipt(Boolean afterReceipt) {
        this.afterReceipt = afterReceipt;
    }

    public Boolean getRequiresAdminApproval() {
        return requiresAdminApproval;
    }

    public void setRequiresAdminApproval(Boolean requiresAdminApproval) {
        this.requiresAdminApproval = requiresAdminApproval;
    }

    public String getAdminStatus() {
        return adminStatus;
    }

    public void setAdminStatus(String adminStatus) {
        this.adminStatus = adminStatus;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public BigDecimal getSupplierShareAmount() {
        return supplierShareAmount;
    }

    public void setSupplierShareAmount(BigDecimal supplierShareAmount) {
        this.supplierShareAmount = supplierShareAmount;
    }

    public BigDecimal getCommissionAmount() {
        return commissionAmount;
    }

    public void setCommissionAmount(BigDecimal commissionAmount) {
        this.commissionAmount = commissionAmount;
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

        public Builder adminResolution(String adminResolution) {
            instance.setAdminResolution(adminResolution);
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

        public Builder adminProcessedAt(Date adminProcessedAt) {
            instance.setAdminProcessedAt(adminProcessedAt);
            return this;
        }

        public Builder afterReceipt(Boolean afterReceipt) {
            instance.setAfterReceipt(afterReceipt);
            return this;
        }

        public Builder requiresAdminApproval(Boolean requiresAdminApproval) {
            instance.setRequiresAdminApproval(requiresAdminApproval);
            return this;
        }

        public Builder adminStatus(String adminStatus) {
            instance.setAdminStatus(adminStatus);
            return this;
        }

        public Builder refundAmount(BigDecimal refundAmount) {
            instance.setRefundAmount(refundAmount);
            return this;
        }

        public Builder supplierShareAmount(BigDecimal supplierShareAmount) {
            instance.setSupplierShareAmount(supplierShareAmount);
            return this;
        }

        public Builder commissionAmount(BigDecimal commissionAmount) {
            instance.setCommissionAmount(commissionAmount);
            return this;
        }

        public ReturnRequestDTO build() {
            return instance;
        }
    }
}
