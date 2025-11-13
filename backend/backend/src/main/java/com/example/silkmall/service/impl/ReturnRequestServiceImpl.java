package com.example.silkmall.service.impl;

import com.example.silkmall.entity.OrderItem;
import com.example.silkmall.entity.ReturnRequest;
import com.example.silkmall.repository.OrderItemRepository;
import com.example.silkmall.repository.ReturnRequestRepository;
import com.example.silkmall.service.ReturnRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class ReturnRequestServiceImpl extends BaseServiceImpl<ReturnRequest, Long> implements ReturnRequestService {
    private static final Set<String> ACTIVE_STATUSES = Set.of("PENDING", "APPROVED");
    private static final Set<String> PROCESSABLE_STATUSES = Set.of("APPROVED", "REJECTED", "COMPLETED");

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderItemRepository orderItemRepository;

    @Autowired
    public ReturnRequestServiceImpl(ReturnRequestRepository returnRequestRepository,
                                    OrderItemRepository orderItemRepository) {
        super(returnRequestRepository);
        this.returnRequestRepository = returnRequestRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public ReturnRequest createReturnRequest(Long orderItemId, ReturnRequest request) {
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new RuntimeException("订单项不存在"));

        if (!"DELIVERED".equals(orderItem.getOrder().getStatus())) {
            throw new RuntimeException("只有已收货的订单才能申请退货");
        }

        if (returnRequestRepository.existsByOrderItemIdAndStatusIn(orderItemId, ACTIVE_STATUSES)) {
            throw new RuntimeException("存在尚未处理的退货申请");
        }

        request.setOrder(orderItem.getOrder());
        request.setOrderItem(orderItem);
        request.setConsumer(orderItem.getOrder().getConsumer());
        request.setProduct(orderItem.getProduct());
        request.setStatus("PENDING");
        request.setProcessedAt(null);

        return returnRequestRepository.save(request);
    }

    @Override
    @Transactional
    public ReturnRequest processReturnRequest(Long id, String status, String resolution) {
        ReturnRequest request = returnRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("退货申请不存在"));

        if (!PROCESSABLE_STATUSES.contains(status)) {
            throw new RuntimeException("不支持的退货处理状态");
        }

        request.setStatus(status);
        request.setResolution(resolution);
        request.setProcessedAt(new Date());

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
}
