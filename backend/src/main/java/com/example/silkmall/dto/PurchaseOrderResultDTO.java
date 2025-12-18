package com.example.silkmall.dto;

import com.example.silkmall.entity.Order;

import java.math.BigDecimal;

public class PurchaseOrderResultDTO {
    private Long id;
    private String orderNo;
    private String status;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private String consumerLookupId;
    private String paymentMethod;
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;

    public static PurchaseOrderResultDTO from(Order order) {
        PurchaseOrderResultDTO dto = new PurchaseOrderResultDTO();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setStatus(order.getStatus());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setTotalQuantity(order.getTotalQuantity());
        dto.setConsumerLookupId(order.getConsumerLookupId());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setRecipientName(order.getRecipientName());
        dto.setRecipientPhone(order.getRecipientPhone());
        return dto;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getConsumerLookupId() {
        return consumerLookupId;
    }

    public void setConsumerLookupId(String consumerLookupId) {
        this.consumerLookupId = consumerLookupId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
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
}
