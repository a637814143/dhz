package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Order;
import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.Product;
import com.example.silkmall.repository.OrderRepository;
import com.example.silkmall.repository.ProductRepository;
import com.example.silkmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl extends BaseServiceImpl<Order, Long> implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    
    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository) {
        super(orderRepository);
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }
    
    @Override
    public Page<Order> findByConsumerId(Long consumerId, Pageable pageable) {
        return orderRepository.findByConsumerId(consumerId, pageable);
    }
    
    @Override
    public Page<Order> findByStatus(String status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }
    
    @Override
    public List<Order> findByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo);
    }
    
    @Transactional
    @Override
    public Order createOrder(Order order) {
        // 生成订单编号
        order.setOrderNo(generateOrderNo());
        
        // 计算订单总金额和总数量
        calculateOrderAmount(order);
        
        // 检查库存并扣减
        checkAndUpdateStock(order);
        
        // 设置订单状态为待支付
        order.setStatus("PENDING_PAYMENT");
        
        return orderRepository.save(order);
    }
    
    @Transactional
    @Override
    public void cancelOrder(Long id) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("只有待支付的订单才能取消");
        }
        
        // 恢复库存
        restoreStock(order);
        
        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }
    
    @Transactional
    @Override
    public void payOrder(Long id, String paymentMethod) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new RuntimeException("只有待支付的订单才能支付");
        }
        
        order.setStatus("PENDING_SHIPMENT");
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(new Date());
        
        orderRepository.save(order);
    }
    
    @Override
    public void shipOrder(Long id) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        
        if (!"PENDING_SHIPMENT".equals(order.getStatus())) {
            throw new RuntimeException("只有待发货的订单才能发货");
        }
        
        order.setStatus("SHIPPING");
        order.setShippingTime(new Date());
        
        orderRepository.save(order);
    }
    
    @Override
    public void deliverOrder(Long id) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        
        if (!"SHIPPING".equals(order.getStatus())) {
            throw new RuntimeException("只有运输中的订单才能确认收货");
        }
        
        order.setStatus("DELIVERED");
        order.setDeliveryTime(new Date());
        
        orderRepository.save(order);
        
        // 订单完成后，可以增加产品销量和消费者积分
        // 这里简化处理，实际项目中可能需要更复杂的逻辑
    }
    
    @Override
    public Order findOrderDetail(Long id) {
        // 这里可以返回包含订单项和产品详情的完整订单信息
        // 实际项目中可能需要使用@EntityGraph或DTO来优化查询
        return findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
    }
    
    // 生成订单编号
    private String generateOrderNo() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 8);
        return timestamp + random;
    }
    
    // 计算订单总金额和总数量
    private void calculateOrderAmount(Order order) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        
        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("产品不存在: " + item.getProduct().getId()));
            
            // 设置商品单价
            item.setUnitPrice(product.getPrice());
            // 计算商品总价
            item.setTotalPrice(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            
            totalAmount = totalAmount.add(item.getTotalPrice());
            totalQuantity += item.getQuantity();
            
            // 设置订单项和订单的关联
            item.setOrder(order);
        }
        
        order.setTotalAmount(totalAmount);
        order.setTotalQuantity(totalQuantity);
    }
    
    // 检查库存并扣减
    private void checkAndUpdateStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("产品不存在: " + item.getProduct().getId()));
            
            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("商品\"" + product.getName() + "\"库存不足");
            }
            
            // 扣减库存
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }
    }
    
    // 恢复库存
    private void restoreStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("产品不存在: " + item.getProduct().getId()));
            
            // 恢复库存
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
    }
}