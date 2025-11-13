package com.example.silkmall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class CreateOrderDTO {
    @NotNull(message = "消费者ID不能为空")
    private Long consumerId;
    
    @NotBlank(message = "收货人姓名不能为空")
    private String recipientName;
    
    @NotBlank(message = "收货人电话不能为空")
    private String recipientPhone;
    
    @NotBlank(message = "收货地址不能为空")
    private String recipientAddress;
    
    private String paymentMethod;
    private String remark;
    
    @NotEmpty(message = "订单项不能为空")
    private List<OrderItemDTO> orderItems;
    
    public Long getConsumerId() {
        return consumerId;
    }
    
    public void setConsumerId(Long consumerId) {
        this.consumerId = consumerId;
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
    
    public String getRecipientAddress() {
        return recipientAddress;
    }
    
    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
    }
    
    public List<OrderItemDTO> getOrderItems() {
        return orderItems;
    }
    
    public void setOrderItems(List<OrderItemDTO> orderItems) {
        this.orderItems = orderItems;
    }
}