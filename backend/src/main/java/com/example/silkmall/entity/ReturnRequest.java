package com.example.silkmall.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "return_requests")
public class ReturnRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String status; // PENDING, APPROVED, REJECTED, COMPLETED

    @Column(length = 1000)
    private String reason;

    @Column(length = 1000)
    private String resolution;

    @Column(length = 1000)
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id")
    @JsonIgnore
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumer_id")
    private Consumer consumer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @PrePersist
    protected void onCreate() {
        requestedAt = new Date();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
