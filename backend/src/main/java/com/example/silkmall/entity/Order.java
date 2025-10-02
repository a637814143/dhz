package com.example.silkmall.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String orderNo;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private String status; // 订单状态：待支付、待发货、待收货、已完成、已取消等
    private String paymentMethod;
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;
    private Date orderTime;
    private Date paymentTime;
    private Date shippingTime;
    private Date deliveryTime;
    
    @ManyToOne
    @JoinColumn(name = "consumer_id")
    private Consumer consumer;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;
    
    @PrePersist
    protected void onCreate() {
        orderTime = new Date();
    }
    
    // Getters and Setters
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
    
    public Consumer getConsumer() {
        return consumer;
    }
    
    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }
    
    public List<OrderItem> getOrderItems() {
        return orderItems;
    }
    
    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
}