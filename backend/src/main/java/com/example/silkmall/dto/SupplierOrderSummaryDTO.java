package com.example.silkmall.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class SupplierOrderSummaryDTO {
    private Long id;
    private String orderNo;
    private String status;
    private Integer totalQuantity;
    private BigDecimal totalAmount;
    private Integer supplierTotalQuantity;
    private BigDecimal supplierTotalAmount;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private Date orderTime;
    private Date paymentTime;
    private Date shippingTime;
    private Date deliveryTime;
    private Date inTransitTime;
    private boolean canShip;
    private boolean canMarkDelivered;
    private boolean mixedSuppliers;
    private List<SupplierOrderItemDTO> items;

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

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getSupplierTotalQuantity() {
        return supplierTotalQuantity;
    }

    public void setSupplierTotalQuantity(Integer supplierTotalQuantity) {
        this.supplierTotalQuantity = supplierTotalQuantity;
    }

    public BigDecimal getSupplierTotalAmount() {
        return supplierTotalAmount;
    }

    public void setSupplierTotalAmount(BigDecimal supplierTotalAmount) {
        this.supplierTotalAmount = supplierTotalAmount;
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

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
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

    public boolean isCanShip() {
        return canShip;
    }

    public void setCanShip(boolean canShip) {
        this.canShip = canShip;
    }

    public boolean isCanMarkDelivered() {
        return canMarkDelivered;
    }

    public void setCanMarkDelivered(boolean canMarkDelivered) {
        this.canMarkDelivered = canMarkDelivered;
    }

    public boolean isMixedSuppliers() {
        return mixedSuppliers;
    }

    public void setMixedSuppliers(boolean mixedSuppliers) {
        this.mixedSuppliers = mixedSuppliers;
    }

    public List<SupplierOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<SupplierOrderItemDTO> items) {
        this.items = items;
    }
}
