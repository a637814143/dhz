package com.example.silkmall.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class WeeklyOrderDTO {
    private Long id;
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private String status;
    private Date orderTime;
    private Date paymentTime;
    private List<WeeklyOrderItemDTO> items;

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

    public List<WeeklyOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<WeeklyOrderItemDTO> items) {
        this.items = items;
    }
}
