package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Order;
import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.ReturnRequest;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.repository.OrderItemRepository;
import com.example.silkmall.repository.OrderRepository;
import com.example.silkmall.repository.ReturnRequestRepository;
import com.example.silkmall.service.OrderService;
import com.example.silkmall.service.ReturnRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Objects;
import static com.example.silkmall.common.OrderStatuses.AWAITING_RECEIPT;
import static com.example.silkmall.common.OrderStatuses.IN_TRANSIT;
import static com.example.silkmall.common.OrderStatuses.PENDING_SHIPMENT;
import static com.example.silkmall.common.OrderStatuses.SHIPPED;
import static com.example.silkmall.common.OrderStatuses.CANCELLED;
import static com.example.silkmall.common.OrderStatuses.REVOKED;

@Service
public class ReturnRequestServiceImpl extends BaseServiceImpl<ReturnRequest, Long> implements ReturnRequestService {
    private static final Set<String> ACTIVE_STATUSES = Set.of("PENDING", "APPROVED");
    private static final Set<String> RETURNABLE_ORDER_STATUSES =
            Set.of(PENDING_SHIPMENT, SHIPPED, IN_TRANSIT, AWAITING_RECEIPT);
    private static final Set<String> PROCESSABLE_STATUSES = Set.of("APPROVED", "REJECTED", "COMPLETED");
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String CANCELLED_BILL_LABEL = "账单已取消";

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    @Autowired
    public ReturnRequestServiceImpl(ReturnRequestRepository returnRequestRepository,
                                    OrderItemRepository orderItemRepository,
                                    OrderRepository orderRepository,
                                    OrderService orderService) {
        super(returnRequestRepository);
        this.returnRequestRepository = returnRequestRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
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

        if (order.getConsumerConfirmationTime() != null) {
            throw new RuntimeException("消费者已确认收货，无法退货");
        }

        String orderStatus = order.getStatus();
        String normalizedStatus = orderStatus == null ? null : orderStatus.trim();
        if (normalizedStatus == null || normalizedStatus.isEmpty()
                || !RETURNABLE_ORDER_STATUSES.contains(normalizedStatus)) {
            throw new RuntimeException("当前订单状态不支持退货");
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
        request.setStatus("PENDING");
        request.setProcessedAt(null);

        return returnRequestRepository.save(request);
    }

    @Override
    @Transactional
    public ReturnRequest processReturnRequest(Long id, String status, String resolution, CustomUserDetails actor) {
        ReturnRequest request = returnRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("退货申请不存在"));

        String normalizedStatus = status == null ? null : status.trim().toUpperCase();
        if (normalizedStatus == null || normalizedStatus.isEmpty()) {
            throw new RuntimeException("退货处理状态不能为空");
        }

        if (!PROCESSABLE_STATUSES.contains(normalizedStatus)) {
            throw new RuntimeException("不支持的退货处理状态");
        }

        if (!isAdmin(actor)) {
            if (actor == null || actor.getUserType() == null || !"supplier".equalsIgnoreCase(actor.getUserType())) {
                throw new RuntimeException("只有对应商家或管理员可以处理退货");
            }
            if (request.getProduct() == null || request.getProduct().getSupplier() == null
                    || !Objects.equals(request.getProduct().getSupplier().getId(), actor.getId())) {
                throw new RuntimeException("无法处理其他商家的退货申请");
            }
            if (STATUS_COMPLETED.equalsIgnoreCase(normalizedStatus)) {
                throw new RuntimeException("只有管理员可以完成退货");
            }
            if ("COMPLETED".equalsIgnoreCase(request.getStatus())) {
                throw new RuntimeException("退货申请已由管理员完成");
            }
        }

        String previousStatus = request.getStatus();
        request.setStatus(normalizedStatus);
        String normalizedResolution = resolution == null ? null : resolution.trim();
        if (normalizedResolution != null && normalizedResolution.isEmpty()) {
            normalizedResolution = null;
        }
        if (normalizedResolution == null && STATUS_APPROVED.equals(normalizedStatus)) {
            normalizedResolution = "供应商已确认退货";
        }
        request.setResolution(normalizedResolution);
        request.setProcessedAt(new Date());

        if (STATUS_APPROVED.equals(normalizedStatus)
                && !STATUS_APPROVED.equalsIgnoreCase(previousStatus)) {
            finalizeApprovedReturn(request);
        }

        return returnRequestRepository.save(request);
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

    private boolean isAdmin(CustomUserDetails actor) {
        return actor != null && "admin".equalsIgnoreCase(actor.getUserType());
    }

    private void finalizeApprovedReturn(ReturnRequest request) {
        Order order = resolveOrder(request);
        if (order == null || order.getId() == null) {
            throw new RuntimeException("退货订单信息不完整");
        }

        if (order.getConsumerConfirmationTime() != null) {
            throw new RuntimeException("消费者已确认收货，无法完成退货");
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
}
