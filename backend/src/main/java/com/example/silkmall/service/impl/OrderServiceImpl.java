package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Admin;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Order;
import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.Product;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.entity.ProductSizeAllocation;
import com.example.silkmall.repository.ConsumerRepository;
import com.example.silkmall.repository.OrderRepository;
import com.example.silkmall.repository.ProductRepository;
import com.example.silkmall.repository.ProductSizeAllocationRepository;
import com.example.silkmall.repository.SupplierRepository;
import com.example.silkmall.repository.AdminRepository;
import com.example.silkmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import static com.example.silkmall.common.OrderStatuses.*;

@Service
public class OrderServiceImpl extends BaseServiceImpl<Order, Long> implements OrderService {

    private static final String PAYOUT_PENDING = "待批准";
    private static final String PAYOUT_APPROVED = "已批准";
    private static final String PAYOUT_REFUNDED = "已退款";

    private static final BigDecimal DEFAULT_WALLET_BALANCE = BigDecimal.valueOf(1000L);
    private static final BigDecimal ADMIN_COMMISSION_RATE = new BigDecimal("0.05");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductSizeAllocationRepository productSizeAllocationRepository;
    private final ConsumerRepository consumerRepository;
    private final SupplierRepository supplierRepository;
    private final AdminRepository adminRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            ProductSizeAllocationRepository productSizeAllocationRepository,
                            ConsumerRepository consumerRepository,
                            SupplierRepository supplierRepository,
                            AdminRepository adminRepository) {
        super(orderRepository);
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.productSizeAllocationRepository = productSizeAllocationRepository;
        this.consumerRepository = consumerRepository;
        this.supplierRepository = supplierRepository;
        this.adminRepository = adminRepository;
    }
    
    @Override
    public Page<Order> findByConsumerId(Long consumerId, Pageable pageable) {
        Pageable candidate = pageable == null ? Pageable.unpaged() : pageable;

        Sort sort = candidate.getSort();
        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.DESC, "orderTime");
        }

        Pageable sortedPageable;
        if (candidate.isPaged()) {
            sortedPageable = PageRequest.of(candidate.getPageNumber(), candidate.getPageSize(), sort);
        } else {
            sortedPageable = PageRequest.of(0, 20, sort);
        }

        return orderRepository.findByConsumerId(consumerId, sortedPageable);
    }
    
    @Override
    public Page<Order> findByStatus(String status, Pageable pageable) {
        return orderRepository.findByStatus(status, pageable);
    }

    @Override
    public Page<Order> findBySupplierId(Long supplierId, Pageable pageable) {
        if (supplierId == null) {
            throw new RuntimeException("供应商ID不能为空");
        }

        Pageable candidate = pageable == null ? Pageable.unpaged() : pageable;

        Sort sort = candidate.getSort();
        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.DESC, "orderTime");
        }

        Pageable sortedPageable;
        if (candidate.isPaged()) {
            sortedPageable = PageRequest.of(candidate.getPageNumber(), candidate.getPageSize(), sort);
        } else {
            sortedPageable = PageRequest.of(0, 20, sort);
        }

        return orderRepository.findDistinctByOrderItems_Product_Supplier_Id(supplierId, sortedPageable);
    }

    @Override
    public List<Order> findByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo);
    }

    @Override
    public List<Order> findByConsumerLookupId(String lookupId) {
        return orderRepository.findByConsumerLookupId(lookupId);
    }

    @Override
    public Page<Order> findAllForAdmin(Boolean consumerConfirmed, String orderNo, Pageable pageable) {
        Pageable resolved = resolveAdminPageable(pageable);
        String trimmedOrderNo = orderNo == null ? null : orderNo.trim();
        if (trimmedOrderNo != null && !trimmedOrderNo.isEmpty()) {
            Page<Order> result = orderRepository.findByOrderNoContainingIgnoreCase(trimmedOrderNo, resolved);
            if (consumerConfirmed == null) {
                return result;
            }
            boolean expectedConfirmation = Boolean.TRUE.equals(consumerConfirmed);
            List<Order> filtered = result.getContent()
                    .stream()
                    .filter(order -> (order.getConsumerConfirmationTime() != null) == expectedConfirmation)
                    .collect(Collectors.toList());
            return new PageImpl<>(filtered, resolved, filtered.size());
        }
        if (Boolean.TRUE.equals(consumerConfirmed)) {
            return orderRepository.findByConsumerConfirmationTimeIsNotNull(resolved);
        }
        if (Boolean.FALSE.equals(consumerConfirmed)) {
            return orderRepository.findByConsumerConfirmationTimeIsNull(resolved);
        }
        return orderRepository.findAllBy(resolved);
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

        order.setStatus(PENDING_PAYMENT);
        order.setPayoutStatus(null);
        order.setManagingAdmin(null);
        order.setAdminHoldingAmount(BigDecimal.ZERO);
        order.setPaymentTime(null);
        order.setShippingTime(null);
        order.setDeliveryTime(null);
        order.setInTransitTime(null);
        order.setConsumerConfirmationTime(null);
        order.setAdminApprovalTime(null);
        Order persisted = orderRepository.save(order);

        if (shouldAutoPay(persisted)) {
            return processPayment(persisted, persisted.getPaymentMethod());
        }

        return persisted;
    }
    
    @Transactional
    @Override
    public void cancelOrder(Long id) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!PENDING_PAYMENT.equals(order.getStatus())) {
            throw new RuntimeException("只有待支付的订单才能取消");
        }

        // 恢复库存
        restoreStock(order);

        order.setStatus(CANCELLED);
        order.setPayoutStatus(null);
        order.setManagingAdmin(null);
        order.setAdminHoldingAmount(BigDecimal.ZERO);
        order.setInTransitTime(null);
        order.setConsumerConfirmationTime(null);
        order.setAdminApprovalTime(null);
        orderRepository.save(order);
    }

    @Transactional
    @Override
    public void revokeOrder(Long id) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (REVOKED.equals(order.getStatus()) || CANCELLED.equals(order.getStatus())) {
            throw new RuntimeException("订单已撤销或已取消");
        }

        Consumer consumer = attachConsumer(order);
        Admin admin = attachAdmin(order);
        BigDecimal totalAmount = order.getTotalAmount();
        if (totalAmount == null) {
            calculateOrderAmount(order);
            totalAmount = order.getTotalAmount();
        }

        restoreStock(order);

        BigDecimal recoveredFromSuppliers = BigDecimal.ZERO;
        if (PAYOUT_APPROVED.equals(order.getPayoutStatus())) {
            recoveredFromSuppliers = rollbackSupplierDistribution(order);
        }

        if (admin != null && totalAmount != null) {
            BigDecimal adminBalance = resolveBalance(admin.getWalletBalance());
            if (recoveredFromSuppliers.compareTo(BigDecimal.ZERO) > 0) {
                adminBalance = adminBalance.add(recoveredFromSuppliers);
            }
            BigDecimal updatedAdmin = adminBalance.subtract(totalAmount);
            if (updatedAdmin.compareTo(BigDecimal.ZERO) < 0) {
                updatedAdmin = BigDecimal.ZERO;
            }
            admin.setWalletBalance(updatedAdmin);
            adminRepository.save(admin);
        }

        if (consumer != null && totalAmount != null) {
            BigDecimal balance = resolveBalance(consumer.getWalletBalance());
            consumer.setWalletBalance(balance.add(totalAmount));
            consumerRepository.save(consumer);
        }

        order.setPayoutStatus(PAYOUT_REFUNDED);
        order.setAdminHoldingAmount(BigDecimal.ZERO);
        order.setAdminApprovalTime(null);
        order.setConsumerConfirmationTime(null);
        order.setStatus(REVOKED);
        orderRepository.save(order);
    }
    
    @Transactional
    @Override
    public void payOrder(Long id, String paymentMethod) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        processPayment(order, paymentMethod);
    }
    
    @Override
    public void shipOrder(Long id) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!PENDING_SHIPMENT.equals(order.getStatus())) {
            throw new RuntimeException("只有待发货的订单才能发货");
        }

        order.setStatus(SHIPPED);
        order.setShippingTime(new Date());

        orderRepository.save(order);
    }

    @Transactional
    @Override
    public void supplierShipOrder(Long id, Long supplierId) {
        if (supplierId == null) {
            throw new RuntimeException("供应商信息缺失");
        }

        Order order = orderRepository.findDetailedById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!PENDING_SHIPMENT.equals(order.getStatus())) {
            throw new RuntimeException("只有待发货的订单才能发货");
        }

        List<OrderItem> items = order.getOrderItems();
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("订单缺少商品信息");
        }

        boolean hasSupplierItems = false;
        for (OrderItem item : items) {
            Product product = item.getProduct();
            if (product == null) {
                product = productRepository.findById(item.getProduct().getId())
                        .orElseThrow(() -> new RuntimeException("产品不存在: " + item.getProduct().getId()));
                item.setProduct(product);
            }

            Supplier supplier = product.getSupplier();
            Long itemSupplierId = supplier == null ? null : supplier.getId();
            if (itemSupplierId == null) {
                throw new RuntimeException("商品缺少供应商信息，无法发货");
            }
            if (!itemSupplierId.equals(supplierId)) {
                throw new RuntimeException("订单包含其他供应商的商品，无法确认发货");
            }
            hasSupplierItems = true;
        }

        if (!hasSupplierItems) {
            throw new RuntimeException("订单中没有该供应商的商品");
        }

        order.setStatus(SHIPPED);
        order.setShippingTime(new Date());

        orderRepository.save(order);
    }

    @Override
    public void markInTransit(Long id) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!SHIPPED.equals(order.getStatus())) {
            throw new RuntimeException("只有已发货的订单才能更新为运送中");
        }

        order.setStatus(IN_TRANSIT);
        order.setInTransitTime(new Date());

        orderRepository.save(order);
    }

    @Override
    public void deliverOrder(Long id) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!IN_TRANSIT.equals(order.getStatus())) {
            throw new RuntimeException("只有运送中的订单才能更新为待收货");
        }

        order.setStatus(AWAITING_RECEIPT);
        order.setDeliveryTime(new Date());

        orderRepository.save(order);

        // 订单完成后，可以增加产品销量和消费者积分
        // 这里简化处理，实际项目中可能需要更复杂的逻辑
    }

    @Transactional
    @Override
    public void confirmReceipt(Long id) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        String status = order.getStatus();
        if (!AWAITING_RECEIPT.equals(status) && !SHIPPED.equals(status) && !IN_TRANSIT.equals(status)) {
            throw new RuntimeException("只有已发货的订单才能确认收货");
        }

        Date now = new Date();
        if (order.getShippingTime() == null) {
            order.setShippingTime(now);
        }
        if (order.getInTransitTime() == null && (SHIPPED.equals(status) || IN_TRANSIT.equals(status))) {
            order.setInTransitTime(now);
        }
        if (order.getDeliveryTime() == null) {
            order.setDeliveryTime(now);
        }

        order.setStatus(DELIVERED);
        order.setConsumerConfirmationTime(now);

        orderRepository.save(order);

        if (PAYOUT_PENDING.equals(order.getPayoutStatus())) {
            finalizePayout(order);
        }
    }

    @Transactional
    @Override
    public void approvePayout(Long id) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        if (!DELIVERED.equals(order.getStatus())) {
            throw new RuntimeException("只有已收货的订单才能批准货款");
        }

        if (!PAYOUT_PENDING.equals(order.getPayoutStatus())) {
            throw new RuntimeException("当前订单没有待批准的货款");
        }

        if (order.getConsumerConfirmationTime() == null) {
            throw new RuntimeException("消费者尚未确认收货，无法结算");
        }

        finalizePayout(order);
    }

    private void finalizePayout(Order order) {
        if (!PAYOUT_PENDING.equals(order.getPayoutStatus())) {
            throw new RuntimeException("当前订单没有待批准的货款");
        }

        if (order.getConsumerConfirmationTime() == null) {
            throw new RuntimeException("消费者尚未确认收货，无法结算");
        }

        Admin admin = attachAdmin(order);
        if (admin == null) {
            throw new RuntimeException("订单缺少资金托管管理员");
        }

        BigDecimal totalAmount = order.getTotalAmount();
        if (totalAmount == null) {
            calculateOrderAmount(order);
            totalAmount = order.getTotalAmount();
        }
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        BigDecimal holdingAmount = Optional.ofNullable(order.getAdminHoldingAmount()).orElse(totalAmount);
        if (holdingAmount == null || holdingAmount.compareTo(BigDecimal.ZERO) < 0) {
            holdingAmount = BigDecimal.ZERO;
        }

        BigDecimal commission = totalAmount.multiply(ADMIN_COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
        if (commission.compareTo(holdingAmount) > 0) {
            commission = holdingAmount;
        }

        BigDecimal payoutPool = holdingAmount.subtract(commission).setScale(2, RoundingMode.HALF_UP);
        if (payoutPool.compareTo(BigDecimal.ZERO) < 0) {
            payoutPool = BigDecimal.ZERO;
        }

        BigDecimal adminBalance = resolveBalance(admin.getWalletBalance());
        boolean hasHeldFunds = holdingAmount.compareTo(BigDecimal.ZERO) > 0
                && adminBalance.compareTo(holdingAmount) >= 0;

        BigDecimal updatedAdminBalance;
        if (hasHeldFunds) {
            if (payoutPool.compareTo(BigDecimal.ZERO) > 0 && adminBalance.compareTo(payoutPool) < 0) {
                throw new RuntimeException("管理员钱包余额不足，无法批准付款");
            }
            BigDecimal baseBalance = adminBalance.subtract(holdingAmount);
            if (baseBalance.compareTo(BigDecimal.ZERO) < 0) {
                baseBalance = BigDecimal.ZERO;
            }
            updatedAdminBalance = baseBalance.add(commission);
        } else {
            // 如果付款时未将货款托管到管理员钱包，则在结算时仅发放提成
            updatedAdminBalance = adminBalance.add(commission);
        }

        admin.setWalletBalance(updatedAdminBalance);
        adminRepository.save(admin);

        BigDecimal supplierDistribution = hasHeldFunds
                ? payoutPool
                : totalAmount.subtract(commission).setScale(2, RoundingMode.HALF_UP);
        if (supplierDistribution.compareTo(BigDecimal.ZERO) < 0) {
            supplierDistribution = BigDecimal.ZERO;
        }

        distributeToSuppliers(order, supplierDistribution);

        order.setPayoutStatus(PAYOUT_APPROVED);
        order.setAdminHoldingAmount(BigDecimal.ZERO);
        order.setAdminApprovalTime(new Date());

        orderRepository.save(order);
    }

    private boolean shouldAutoPay(Order order) {
        if (order == null) {
            return false;
        }
        if (order.getConsumer() == null || order.getConsumer().getId() == null) {
            return false;
        }
        String method = order.getPaymentMethod();
        return method != null && !method.trim().isEmpty();
    }

    private Order processPayment(Order order, String paymentMethod) {
        if (!PENDING_PAYMENT.equals(order.getStatus())) {
            throw new RuntimeException("只有待支付的订单才能支付");
        }

        String normalizedMethod = paymentMethod == null ? null : paymentMethod.trim();
        if (normalizedMethod == null || normalizedMethod.isEmpty()) {
            throw new RuntimeException("支付方式不能为空");
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
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        BigDecimal consumerBalance = resolveBalance(consumer.getWalletBalance());
        if (consumerBalance.compareTo(totalAmount) < 0) {
            throw new RuntimeException("钱包余额不足，无法完成支付");
        }

        Admin admin = attachAdmin(order);
        if (admin == null) {
            admin = selectOrderAdmin();
        }
        BigDecimal adminBalance = resolveBalance(admin.getWalletBalance());

        order.setStatus(PENDING_SHIPMENT);
        order.setPaymentMethod(normalizedMethod);
        order.setPaymentTime(new Date());
        order.setShippingTime(null);
        order.setDeliveryTime(null);
        order.setInTransitTime(null);
        order.setConsumerConfirmationTime(null);
        order.setAdminApprovalTime(null);
        order.setManagingAdmin(admin);
        order.setPayoutStatus(PAYOUT_PENDING);
        order.setAdminHoldingAmount(totalAmount);

        consumer.setWalletBalance(consumerBalance.subtract(totalAmount));
        consumerRepository.save(consumer);

        admin.setWalletBalance(adminBalance.add(totalAmount));
        adminRepository.save(admin);

        return orderRepository.save(order);
    }

    @Transactional
    @Override
    public Order updateContactInfo(Long id, String shippingAddress, String recipientName, String recipientPhone) {
        Order order = findById(id)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        order.setShippingAddress(shippingAddress);
        order.setRecipientName(recipientName);
        order.setRecipientPhone(recipientPhone);

        return orderRepository.save(order);
    }

    @Override
    public Order findOrderDetail(Long id) {
        return orderRepository.findDetailedById(id)
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

            List<ProductSizeAllocation> allocations = product.getSizeAllocations();
            String sizeLabel = item.getSizeLabel();
            if (allocations != null && !allocations.isEmpty()) {
                if (sizeLabel == null || sizeLabel.isBlank()) {
                    throw new RuntimeException("商品\"" + product.getName() + "\"需要选择尺码");
                }
                ProductSizeAllocation matched = allocations.stream()
                        .filter(allocation -> sizeLabel.equalsIgnoreCase(allocation.getSizeLabel()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("尺码" + sizeLabel + "不存在"));
                if (matched.getQuantity() < item.getQuantity()) {
                    throw new RuntimeException("尺码" + sizeLabel + "库存不足");
                }
                matched.setQuantity(matched.getQuantity() - item.getQuantity());
                productSizeAllocationRepository.save(matched);
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

            List<ProductSizeAllocation> allocations = product.getSizeAllocations();
            String sizeLabel = item.getSizeLabel();
            if (allocations != null && !allocations.isEmpty() && sizeLabel != null && !sizeLabel.isBlank()) {
                allocations.stream()
                        .filter(allocation -> sizeLabel.equalsIgnoreCase(allocation.getSizeLabel()))
                        .findFirst()
                        .ifPresent(allocation -> {
                            allocation.setQuantity(allocation.getQuantity() + item.getQuantity());
                            productSizeAllocationRepository.save(allocation);
                        });
            }
        }
    }

    private void distributeToSuppliers(Order order, BigDecimal payoutPool) {
        BigDecimal effectivePool = payoutPool == null ? BigDecimal.ZERO : payoutPool;
        if (effectivePool.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Map<Long, BigDecimal> payouts = calculateSupplierPayouts(order, effectivePool);
        for (Map.Entry<Long, BigDecimal> entry : payouts.entrySet()) {
            Supplier supplier = supplierRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("供应商不存在: " + entry.getKey()));
            BigDecimal current = supplier.getWalletBalance() == null
                    ? BigDecimal.valueOf(1000L)
                    : supplier.getWalletBalance();
            supplier.setWalletBalance(current.add(entry.getValue()));
            supplierRepository.save(supplier);
        }
    }

    private BigDecimal rollbackSupplierDistribution(Order order) {
        BigDecimal totalAmount = Optional.ofNullable(order.getTotalAmount()).orElse(BigDecimal.ZERO);
        BigDecimal commission = totalAmount.multiply(ADMIN_COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal payoutPool = totalAmount.subtract(commission);
        if (payoutPool.compareTo(BigDecimal.ZERO) < 0) {
            payoutPool = BigDecimal.ZERO;
        }

        Map<Long, BigDecimal> payouts = calculateSupplierPayouts(order, payoutPool);
        BigDecimal recovered = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : payouts.entrySet()) {
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
            recovered = recovered.add(entry.getValue());
        }
        return recovered;
    }

    private Map<Long, BigDecimal> calculateSupplierPayouts(Order order, BigDecimal payoutPool) {
        Map<Long, BigDecimal> baseAmounts = collectSupplierAmounts(order);
        if (baseAmounts.isEmpty() || payoutPool.compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of();
        }

        BigDecimal totalBase = baseAmounts.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalBase.compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of();
        }

        LinkedHashMap<Long, BigDecimal> payouts = new LinkedHashMap<>();
        BigDecimal distributed = BigDecimal.ZERO;
        Long lastSupplierId = null;

        for (Map.Entry<Long, BigDecimal> entry : baseAmounts.entrySet()) {
            lastSupplierId = entry.getKey();
            BigDecimal ratio = entry.getValue().divide(totalBase, 10, RoundingMode.HALF_UP);
            BigDecimal share = payoutPool.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
            payouts.put(entry.getKey(), share);
            distributed = distributed.add(share);
        }

        if (lastSupplierId != null) {
            BigDecimal remainder = payoutPool.subtract(distributed);
            if (remainder.compareTo(BigDecimal.ZERO) != 0) {
                payouts.computeIfPresent(lastSupplierId, (id, amount) -> amount.add(remainder));
            }
        }

        return payouts;
    }

    private Pageable resolveAdminPageable(Pageable pageable) {
        Pageable candidate = pageable == null ? Pageable.unpaged() : pageable;
        Sort sort = candidate.getSort();
        if (sort == null || sort.isUnsorted()) {
            sort = Sort.by(Sort.Direction.DESC, "orderTime");
        }
        if (candidate.isPaged()) {
            return PageRequest.of(candidate.getPageNumber(), candidate.getPageSize(), sort);
        }
        return PageRequest.of(0, 20, sort);
    }

    private Map<Long, BigDecimal> collectSupplierAmounts(Order order) {
        Map<Long, BigDecimal> earnings = new LinkedHashMap<>();
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
                BigDecimal totalPrice = item.getTotalPrice() == null ? BigDecimal.ZERO : item.getTotalPrice();
                earnings.merge(supplier.getId(), totalPrice, BigDecimal::add);
            }
        }
        if (earnings.isEmpty()) {
            return earnings;
        }
        return new LinkedHashMap<>(new TreeMap<>(earnings));
    }

    private Consumer attachConsumer(Order order) {
        if (order.getConsumer() == null || order.getConsumer().getId() == null) {
            return null;
        }
        Consumer consumer = consumerRepository.findById(order.getConsumer().getId())
                .orElseThrow(() -> new RuntimeException("消费者不存在: " + order.getConsumer().getId()));
        if (consumer.getWalletBalance() == null) {
            consumer.setWalletBalance(DEFAULT_WALLET_BALANCE);
        }
        order.setConsumer(consumer);
        return consumer;
    }

    private Admin attachAdmin(Order order) {
        if (order.getManagingAdmin() == null || order.getManagingAdmin().getId() == null) {
            return null;
        }
        Admin admin = adminRepository.findById(order.getManagingAdmin().getId())
                .orElseThrow(() -> new RuntimeException("管理员不存在: " + order.getManagingAdmin().getId()));
        order.setManagingAdmin(admin);
        return admin;
    }

    private Admin selectOrderAdmin() {
        return adminRepository.findTopByOrderByIdAsc()
                .orElseThrow(() -> new RuntimeException("系统未配置管理员，无法处理资金"));
    }

    private BigDecimal resolveBalance(BigDecimal balance) {
        return balance == null ? DEFAULT_WALLET_BALANCE : balance;
    }

    private String generateConsumerLookupId() {
        return "C" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12).toUpperCase();
    }
}