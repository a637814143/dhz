package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Admin;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Order;
import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.Product;
import com.example.silkmall.entity.ReturnRequest;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.repository.AdminRepository;
import com.example.silkmall.repository.ConsumerRepository;
import com.example.silkmall.repository.OrderItemRepository;
import com.example.silkmall.repository.OrderRepository;
import com.example.silkmall.repository.ReturnRequestRepository;
import com.example.silkmall.repository.SupplierRepository;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.service.OrderService;
import com.example.silkmall.service.ReturnRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.silkmall.common.OrderStatuses.AWAITING_RECEIPT;
import static com.example.silkmall.common.OrderStatuses.CANCELLED;
import static com.example.silkmall.common.OrderStatuses.DELIVERED;
import static com.example.silkmall.common.OrderStatuses.IN_TRANSIT;
import static com.example.silkmall.common.OrderStatuses.PENDING_SHIPMENT;
import static com.example.silkmall.common.OrderStatuses.REVOKED;
import static com.example.silkmall.common.OrderStatuses.SHIPPED;

@Service
public class ReturnRequestServiceImpl extends BaseServiceImpl<ReturnRequest, Long> implements ReturnRequestService {
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUPPLIER_APPROVED = "APPROVED";
    private static final String STATUS_SUPPLIER_REJECTED = "REJECTED";
    private static final String STATUS_AWAITING_ADMIN = "AWAITING_ADMIN";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private static final String ADMIN_STATUS_PENDING = "PENDING";
    private static final String ADMIN_STATUS_APPROVED = "APPROVED";
    private static final String ADMIN_STATUS_REJECTED = "REJECTED";

    private static final Set<String> ACTIVE_STATUSES = Set.of(STATUS_PENDING, STATUS_AWAITING_ADMIN);
    private static final Set<String> RETURNABLE_ORDER_STATUSES =
            Set.of(PENDING_SHIPMENT, SHIPPED, IN_TRANSIT, AWAITING_RECEIPT);
    private static final Set<String> SUPPLIER_PROCESSABLE_STATUSES = Set.of(STATUS_SUPPLIER_APPROVED, STATUS_SUPPLIER_REJECTED);

    private static final BigDecimal DEFAULT_WALLET_BALANCE = BigDecimal.valueOf(1000L);
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.05");
    private static final String CANCELLED_BILL_LABEL = "账单已取消";

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final SupplierRepository supplierRepository;
    private final ConsumerRepository consumerRepository;
    private final AdminRepository adminRepository;
    private final OrderService orderService;

    @Autowired
    public ReturnRequestServiceImpl(ReturnRequestRepository returnRequestRepository,
                                    OrderItemRepository orderItemRepository,
                                    OrderRepository orderRepository,
                                    SupplierRepository supplierRepository,
                                    ConsumerRepository consumerRepository,
                                    AdminRepository adminRepository,
                                    OrderService orderService) {
        super(returnRequestRepository);
        this.returnRequestRepository = returnRequestRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.supplierRepository = supplierRepository;
        this.consumerRepository = consumerRepository;
        this.adminRepository = adminRepository;
        this.orderService = orderService;
    }

    @Override
    @Transactional
    public ReturnRequest createReturnRequest(Long orderItemId, ReturnRequest request) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("订单项不存在"));

        Order order = orderItem.getOrder();
        if (order == null) {
            throw new RuntimeException("退货申请缺少订单信息");
        }

        String orderStatus = order.getStatus();
        String normalizedStatus = orderStatus == null ? null : orderStatus.trim();
        boolean consumerConfirmed = order.getConsumerConfirmationTime() != null;

        if (!consumerConfirmed) {
            if (normalizedStatus == null || normalizedStatus.isEmpty()
                    || !RETURNABLE_ORDER_STATUSES.contains(normalizedStatus)) {
                throw new RuntimeException("当前订单状态不支持退货");
            }
        } else {
            if (!DELIVERED.equals(normalizedStatus)) {
                throw new RuntimeException("订单未完成收货确认，暂不支持售后退货");
            }
        }

        if (CANCELLED.equals(normalizedStatus) || REVOKED.equals(normalizedStatus)) {
            throw new RuntimeException("订单已取消，无法重复申请退货");
        }

        if (returnRequestRepository.existsByOrderItemIdAndStatusIn(orderItemId, ACTIVE_STATUSES)) {
            throw new RuntimeException("存在尚未处理的退货申请");
        }

        request.setOrder(order);
        request.setOrderItem(orderItem);
        request.setConsumer(order.getConsumer());
        request.setProduct(orderItem.getProduct());
        request.setStatus(STATUS_PENDING);
        request.setProcessedAt(null);
        request.setAdminProcessedAt(null);
        request.setAfterReceipt(consumerConfirmed);
        request.setRequiresAdminApproval(consumerConfirmed);
        request.setAdminStatus(consumerConfirmed ? ADMIN_STATUS_PENDING : null);
        request.setAdminResolution(null);

        BigDecimal refundAmount = resolveItemTotal(orderItem);
        request.setRefundAmount(refundAmount);
        BigDecimal commission = calculateCommission(refundAmount);
        request.setCommissionAmount(commission);
        request.setSupplierShareAmount(refundAmount.subtract(commission).setScale(2, RoundingMode.HALF_UP));

        return returnRequestRepository.save(request);
    }

    @Override
    @Transactional
    public ReturnRequest processReturnRequest(Long id,
                                              String status,
                                              String supplierResolution,
                                              String adminResolution,
                                              CustomUserDetails actor) {
        ReturnRequest request = returnRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("退货申请不存在"));

        boolean adminActor = isAdmin(actor);
        String normalizedStatus = normalize(status);
        if (normalizedStatus == null) {
            throw new RuntimeException("退货处理状态不能为空");
        }

        if (adminActor) {
            return handleAdminDecision(request, normalizedStatus, trimToNull(adminResolution));
        }

        if (actor == null || actor.getUserType() == null || !"supplier".equalsIgnoreCase(actor.getUserType())) {
            throw new RuntimeException("只有对应商家或管理员可以处理退货");
        }
        if (request.getProduct() == null || request.getProduct().getSupplier() == null
                || !Objects.equals(request.getProduct().getSupplier().getId(), actor.getId())) {
            throw new RuntimeException("无法处理其他商家的退货申请");
        }
        if (!SUPPLIER_PROCESSABLE_STATUSES.contains(normalizedStatus)) {
            throw new RuntimeException("不支持的退货处理状态");
        }
        if (STATUS_COMPLETED.equalsIgnoreCase(request.getStatus())) {
            throw new RuntimeException("退货申请已完成处理");
        }

        return handleSupplierDecision(request, normalizedStatus, trimToNull(supplierResolution));
    }

    @Override
    public List<ReturnRequest> findByConsumerId(Long consumerId) {
        return returnRequestRepository.findByConsumerId(consumerId);
    }

    @Override
    public List<ReturnRequest> findByOrderId(Long orderId) {
        return returnRequestRepository.findByOrderId(orderId);
    }

    @Override
    public List<ReturnRequest> findBySupplierId(Long supplierId) {
        return returnRequestRepository.findByProduct_Supplier_Id(supplierId);
    }

    @Override
    public List<ReturnRequest> findAdminQueue(String statusFilter) {
        List<ReturnRequest> afterReceipt = returnRequestRepository.findByAfterReceiptTrueOrderByRequestedAtDesc();
        String normalized = normalize(statusFilter);
        if (normalized == null || normalized.equals("PENDING")) {
            return afterReceipt.stream()
                    .filter(request -> STATUS_AWAITING_ADMIN.equalsIgnoreCase(request.getStatus()))
                    .collect(Collectors.toList());
        }
        if (normalized.equals("ALL")) {
            return afterReceipt;
        }
        if (normalized.equals(STATUS_COMPLETED)) {
            return afterReceipt.stream()
                    .filter(request -> STATUS_COMPLETED.equalsIgnoreCase(request.getStatus()))
                    .collect(Collectors.toList());
        }
        if (normalized.equals(STATUS_SUPPLIER_REJECTED)) {
            return afterReceipt.stream()
                    .filter(request -> STATUS_SUPPLIER_REJECTED.equalsIgnoreCase(request.getStatus()))
                    .collect(Collectors.toList());
        }
        return afterReceipt;
    }

    private ReturnRequest handleSupplierDecision(ReturnRequest request, String status, String resolution) {
        request.setProcessedAt(new Date());
        if (STATUS_SUPPLIER_APPROVED.equals(status)) {
            if (Boolean.TRUE.equals(request.getAfterReceipt())) {
                request.setStatus(STATUS_AWAITING_ADMIN);
                request.setRequiresAdminApproval(true);
                request.setAdminStatus(ADMIN_STATUS_PENDING);
                request.setResolution(resolution == null ? "供应商已确认退货" : resolution);
                request.setAdminProcessedAt(null);
                request.setAdminResolution(null);
                ensureRefundBreakdown(request);
                return returnRequestRepository.save(request);
            }

            request.setResolution(resolution == null ? "供应商已确认退货" : resolution);
            finalizePreDeliveryReturn(request);
            request.setStatus(STATUS_COMPLETED);
            request.setRequiresAdminApproval(false);
            request.setAdminStatus(null);
            request.setAdminProcessedAt(new Date());
            request.setAdminResolution(request.getResolution());
            return returnRequestRepository.save(request);
        }

        request.setStatus(STATUS_SUPPLIER_REJECTED);
        request.setResolution(resolution == null ? "供应商拒绝退货" : resolution);
        request.setRequiresAdminApproval(false);
        request.setAdminStatus(ADMIN_STATUS_REJECTED);
        request.setAdminProcessedAt(new Date());
        request.setAdminResolution(request.getResolution());
        return returnRequestRepository.save(request);
    }

    private ReturnRequest handleAdminDecision(ReturnRequest request, String status, String adminResolution) {
        if (!STATUS_COMPLETED.equals(status)) {
            throw new RuntimeException("管理员仅支持确认退货完成");
        }

        if (!Boolean.TRUE.equals(request.getAfterReceipt()) || !Boolean.TRUE.equals(request.getRequiresAdminApproval())) {
            throw new RuntimeException("该退货不需要管理员审批");
        }

        if (!STATUS_AWAITING_ADMIN.equalsIgnoreCase(request.getStatus())) {
            throw new RuntimeException("退货申请尚未到达管理员审批阶段");
        }

        completeAfterReceiptReturn(request, adminResolution);
        return returnRequestRepository.save(request);
    }

    private void finalizePreDeliveryReturn(ReturnRequest request) {
        if (Boolean.TRUE.equals(request.getAfterReceipt())) {
            return;
        }

        Order order = resolveOrder(request);
        if (order == null || order.getId() == null) {
            throw new RuntimeException("退货订单信息不完整");
        }

        String status = order.getStatus() == null ? null : order.getStatus().trim();
        if (CANCELLED.equals(status)) {
            if (!CANCELLED_BILL_LABEL.equals(order.getPayoutStatus())) {
                order.setPayoutStatus(CANCELLED_BILL_LABEL);
                orderRepository.save(order);
            }
            return;
        }

        if (!REVOKED.equals(status)) {
            orderService.revokeOrder(order.getId());
            order = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new RuntimeException("订单不存在"));
        }

        order.setStatus(CANCELLED);
        order.setPayoutStatus(CANCELLED_BILL_LABEL);
        orderRepository.save(order);
        request.setOrder(order);
    }

    private void completeAfterReceiptReturn(ReturnRequest request, String adminResolution) {
        Order order = resolveOrder(request);
        if (order == null || order.getId() == null) {
            throw new RuntimeException("退货订单信息不完整");
        }

        OrderItem orderItem = request.getOrderItem();
        if (orderItem == null || orderItem.getId() == null) {
            throw new RuntimeException("退货申请缺少订单项信息");
        }

        Product product = orderItem.getProduct();
        Supplier supplier = product == null ? null : product.getSupplier();
        if (supplier == null || supplier.getId() == null) {
            throw new RuntimeException("退货申请缺少供应商信息");
        }

        Consumer consumer = order.getConsumer();
        if (consumer == null || consumer.getId() == null) {
            throw new RuntimeException("退货申请缺少消费者信息");
        }

        Admin admin = order.getManagingAdmin();
        if (admin == null || admin.getId() == null) {
            throw new RuntimeException("订单缺少托管管理员，无法完成退款");
        }

        BigDecimal refundAmount = ensureRefundBreakdown(request);
        BigDecimal commission = request.getCommissionAmount();
        BigDecimal supplierShare = request.getSupplierShareAmount();

        Supplier persistedSupplier = supplierRepository.findById(supplier.getId())
                .orElseThrow(() -> new RuntimeException("供应商不存在: " + supplier.getId()));
        Consumer persistedConsumer = consumerRepository.findById(consumer.getId())
                .orElseThrow(() -> new RuntimeException("消费者不存在: " + consumer.getId()));
        BigDecimal supplierContribution = supplierShare == null
                ? refundAmount.setScale(2, RoundingMode.HALF_UP)
                : supplierShare.setScale(2, RoundingMode.HALF_UP);
        if (Boolean.TRUE.equals(request.getAfterReceipt())) {
            BigDecimal normalizedRefund = refundAmount.setScale(2, RoundingMode.HALF_UP);
            supplierContribution = normalizedRefund;
            request.setSupplierShareAmount(normalizedRefund);
            if (commission == null || commission.compareTo(BigDecimal.ZERO) != 0) {
                request.setCommissionAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                commission = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal supplierBalance = resolveBalance(persistedSupplier.getWalletBalance());
        if (supplierBalance.compareTo(supplierContribution) < 0) {
            throw new RuntimeException("供应商钱包余额不足，无法完成退款");
        }
        persistedSupplier.setWalletBalance(supplierBalance.subtract(supplierContribution));
        supplierRepository.save(persistedSupplier);

        BigDecimal consumerBalance = resolveBalance(persistedConsumer.getWalletBalance());
        persistedConsumer.setWalletBalance(consumerBalance.add(refundAmount));
        consumerRepository.save(persistedConsumer);

        request.setStatus(STATUS_COMPLETED);
        request.setAdminStatus(ADMIN_STATUS_APPROVED);
        request.setAdminProcessedAt(new Date());
        request.setAdminResolution(adminResolution == null ? "管理员已确认退款" : adminResolution);
        request.setRequiresAdminApproval(false);

        order.setPayoutStatus("已退款");
        orderRepository.save(order);
    }

    private BigDecimal ensureRefundBreakdown(ReturnRequest request) {
        BigDecimal refundAmount = request.getRefundAmount();
        if (refundAmount == null) {
            refundAmount = resolveItemTotal(request.getOrderItem());
            request.setRefundAmount(refundAmount);
        }

        BigDecimal commission = request.getCommissionAmount();
        BigDecimal supplierShare = request.getSupplierShareAmount();

        if (Boolean.TRUE.equals(request.getAfterReceipt())) {
            BigDecimal normalizedRefund = refundAmount.setScale(2, RoundingMode.HALF_UP);
            if (commission == null || commission.compareTo(BigDecimal.ZERO) != 0) {
                request.setCommissionAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
            if (supplierShare == null || supplierShare.compareTo(normalizedRefund) != 0) {
                request.setSupplierShareAmount(normalizedRefund);
            }
            return refundAmount;
        }

        if (commission == null) {
            commission = calculateCommission(refundAmount);
            request.setCommissionAmount(commission);
        }

        if (supplierShare == null) {
            supplierShare = refundAmount.subtract(commission).setScale(2, RoundingMode.HALF_UP);
            request.setSupplierShareAmount(supplierShare);
        }

        return refundAmount;
    }

    private BigDecimal resolveItemTotal(OrderItem orderItem) {
        if (orderItem == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = orderItem.getTotalPrice();
        if (total == null) {
            BigDecimal unit = orderItem.getUnitPrice();
            Integer quantity = orderItem.getQuantity();
            if (unit != null && quantity != null) {
                total = unit.multiply(BigDecimal.valueOf(quantity));
            }
        }
        if (total == null) {
            total = BigDecimal.ZERO;
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCommission(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal commission = amount.multiply(COMMISSION_RATE).setScale(2, RoundingMode.HALF_UP);
        if (commission.compareTo(amount) > 0) {
            commission = amount;
        }
        return commission;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isAdmin(CustomUserDetails actor) {
        return actor != null && "admin".equalsIgnoreCase(actor.getUserType());
    }

    private Order resolveOrder(ReturnRequest request) {
        Order order = request.getOrder();
        if (order != null) {
            return order;
        }
        if (request.getOrderItem() != null && request.getOrderItem().getOrder() != null) {
            return request.getOrderItem().getOrder();
        }
        return null;
    }

    private BigDecimal resolveBalance(BigDecimal balance) {
        return balance == null ? DEFAULT_WALLET_BALANCE : balance;
    }
}
