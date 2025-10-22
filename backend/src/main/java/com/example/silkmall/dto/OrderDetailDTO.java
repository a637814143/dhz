package com.example.silkmall.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderDetailDTO {
    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private String status;
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;
    private Date orderTime;
    private Date paymentTime;
    private Date shippingTime;
    private Date deliveryTime;
    private Date inTransitTime;
    private Date consumerConfirmationTime;
    private Date adminApprovalTime;
    private String payoutStatus;
    private BigDecimal adminHoldingAmount;
    private Long managingAdminId;
    private String managingAdminName;
    private List<OrderItemDetailDTO> orderItems = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public Date getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(Date orderTime) {
        this.orderTime = orderTime;
    }

    public Date getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(Date paymentTime) {
        this.paymentTime = paymentTime;
    }

    public Date getShippingTime() {
        return shippingTime;
    }

    public void setShippingTime(Date shippingTime) {
        this.shippingTime = shippingTime;
    }

    public Date getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(Date deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public Date getInTransitTime() {
        return inTransitTime;
    }

    public void setInTransitTime(Date inTransitTime) {
        this.inTransitTime = inTransitTime;
    }

    public Date getConsumerConfirmationTime() {
        return consumerConfirmationTime;
    }

    public void setConsumerConfirmationTime(Date consumerConfirmationTime) {
        this.consumerConfirmationTime = consumerConfirmationTime;
    }

    public Date getAdminApprovalTime() {
        return adminApprovalTime;
    }

    public void setAdminApprovalTime(Date adminApprovalTime) {
        this.adminApprovalTime = adminApprovalTime;
    }

    public String getPayoutStatus() {
        return payoutStatus;
    }

    public void setPayoutStatus(String payoutStatus) {
        this.payoutStatus = payoutStatus;
    }

    public BigDecimal getAdminHoldingAmount() {
        return adminHoldingAmount;
    }

    public void setAdminHoldingAmount(BigDecimal adminHoldingAmount) {
        this.adminHoldingAmount = adminHoldingAmount;
    }

    public Long getManagingAdminId() {
        return managingAdminId;
    }

    public void setManagingAdminId(Long managingAdminId) {
        this.managingAdminId = managingAdminId;
    }

    public String getManagingAdminName() {
        return managingAdminName;
    }

    public void setManagingAdminName(String managingAdminName) {
        this.managingAdminName = managingAdminName;
    }

    public List<OrderItemDetailDTO> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItemDetailDTO> orderItems) {
        this.orderItems = orderItems;
    }
}
