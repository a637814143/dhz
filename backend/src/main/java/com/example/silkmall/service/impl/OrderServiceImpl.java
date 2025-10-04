package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Order;
import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.Product;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.repository.ConsumerRepository;
import com.example.silkmall.repository.OrderRepository;
import com.example.silkmall.repository.ProductRepository;
import com.example.silkmall.repository.SupplierRepository;
import com.example.silkmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderServiceImpl extends BaseServiceImpl<Order, Long> implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ConsumerRepository consumerRepository;
    private final SupplierRepository supplierRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            ConsumerRepository consumerRepository,
                            SupplierRepository supplierRepository) {
        super(orderRepository);
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.consumerRepository = consumerRepository;
        this.supplierRepository = supplierRepository;
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

    @Override
    public List<Order> findByConsumerLookupId(String lookupId) {
        return orderRepository.findByConsumerLookupId(lookupId);
    }

    @Transactional
    @Override
    public Order createOrder(Order order) {
        order.setOrderNo(generateOrderNo());
        if (order.getConsumerLookupId() == null || order.getConsumerLookupId().isBlank()) {
            order.setConsumerLookupId(generateConsumerLookupId());
        }

        attachConsumer(order);

        calculateOrderAmount(order);
        checkAndUpdateStock(order);

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
    public void revokeOrder(Long id) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if ("REVOKED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new RuntimeException("订单已撤销或已取消");
        }

        Consumer consumer = attachConsumer(order);
        BigDecimal totalAmount = order.getTotalAmount();
        if (totalAmount == null) {
            calculateOrderAmount(order);
            totalAmount = order.getTotalAmount();
        }

        restoreStock(order);

        if (consumer != null && totalAmount != null) {
            BigDecimal balance = consumer.getWalletBalance() == null
                    ? BigDecimal.valueOf(1000L)
                    : consumer.getWalletBalance();
            consumer.setWalletBalance(balance.add(totalAmount));
            consumerRepository.save(consumer);
        }

        rollbackSupplierDistribution(order);

        order.setStatus("REVOKED");
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

        Consumer consumer = attachConsumer(order);
        if (consumer == null) {
            throw new RuntimeException("订单缺少消费者信息");
        }

        BigDecimal totalAmount = order.getTotalAmount();
        if (totalAmount == null) {
            calculateOrderAmount(order);
            totalAmount = order.getTotalAmount();
        }

        BigDecimal consumerBalance = consumer.getWalletBalance() == null
                ? BigDecimal.valueOf(1000L)
                : consumer.getWalletBalance();
        if (consumerBalance.compareTo(totalAmount) < 0) {
            throw new RuntimeException("钱包余额不足，无法完成支付");
        }

        order.setStatus("PENDING_SHIPMENT");
        order.setPaymentMethod(paymentMethod);
        order.setPaymentTime(new Date());

        consumer.setWalletBalance(consumerBalance.subtract(totalAmount));
        consumerRepository.save(consumer);
        distributeToSuppliers(order);

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

    @Transactional
    @Override
    public Order updateContactInfo(Long id, String shippingAddress, String recipientName, String recipientPhone) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        String status = order.getStatus();
        if (status != null) {
            switch (status) {
                case "DELIVERED", "CANCELLED", "REVOKED" ->
                        throw new RuntimeException("当前状态下无法修改收货信息");
                default -> {
                }
            }
        }

        order.setShippingAddress(normalizeBlankToNull(shippingAddress));
        order.setRecipientName(normalizeBlankToNull(recipientName));
        order.setRecipientPhone(normalizeBlankToNull(recipientPhone));

        return orderRepository.save(order);
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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
            item.setProduct(product);

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

    private void distributeToSuppliers(Order order) {
        Map<Long, BigDecimal> earnings = collectSupplierAmounts(order);
        for (Map.Entry<Long, BigDecimal> entry : earnings.entrySet()) {
            Supplier supplier = supplierRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("供应商不存在: " + entry.getKey()));
            BigDecimal current = supplier.getWalletBalance() == null
                    ? BigDecimal.valueOf(1000L)
                    : supplier.getWalletBalance();
            supplier.setWalletBalance(current.add(entry.getValue()));
            supplierRepository.save(supplier);
        }
    }

    private void rollbackSupplierDistribution(Order order) {
        Map<Long, BigDecimal> earnings = collectSupplierAmounts(order);
        for (Map.Entry<Long, BigDecimal> entry : earnings.entrySet()) {
            Supplier supplier = supplierRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("供应商不存在: " + entry.getKey()));
            BigDecimal current = supplier.getWalletBalance() == null
                    ? BigDecimal.valueOf(1000L)
                    : supplier.getWalletBalance();
            BigDecimal updated = current.subtract(entry.getValue());
            if (updated.compareTo(BigDecimal.ZERO) < 0) {
                updated = BigDecimal.ZERO;
            }
            supplier.setWalletBalance(updated);
            supplierRepository.save(supplier);
        }
    }

    private Map<Long, BigDecimal> collectSupplierAmounts(Order order) {
        Map<Long, BigDecimal> earnings = new HashMap<>();
        if (order.getOrderItems() == null) {
            return earnings;
        }
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            if (product == null) {
                product = productRepository.findById(item.getProduct().getId())
                        .orElseThrow(() -> new RuntimeException("产品不存在: " + item.getProduct().getId()));
            }
            Supplier supplier = product.getSupplier();
            if (supplier != null && supplier.getId() != null && item.getTotalPrice() != null) {
                earnings.merge(supplier.getId(), item.getTotalPrice(), BigDecimal::add);
            }
        }
        return earnings;
    }

    private Consumer attachConsumer(Order order) {
        if (order.getConsumer() == null || order.getConsumer().getId() == null) {
            return null;
        }
        Consumer consumer = consumerRepository.findById(order.getConsumer().getId())
                .orElseThrow(() -> new RuntimeException("消费者不存在: " + order.getConsumer().getId()));
        if (consumer.getWalletBalance() == null) {
            consumer.setWalletBalance(BigDecimal.valueOf(1000L));
        }
        order.setConsumer(consumer);
        return consumer;
    }

    private String generateConsumerLookupId() {
        return "C" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toUpperCase();
    }
}