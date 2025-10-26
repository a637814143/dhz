package com.example.silkmall.service;

import com.example.silkmall.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface OrderService extends BaseService<Order, Long> {
    Page<Order> findByConsumerId(Long consumerId, Pageable pageable);
    Page<Order> findByStatus(String status, Pageable pageable);
    Page<Order> findBySupplierId(Long supplierId, Pageable pageable);
    List<Order> findByOrderNo(String orderNo);
    List<Order> findByConsumerLookupId(String lookupId);
    Page<Order> findAllForAdmin(Boolean consumerConfirmed, Pageable pageable);
    Order createOrder(Order order);
    void cancelOrder(Long id);
    void payOrder(Long id, String paymentMethod);
    void revokeOrder(Long id);
    void shipOrder(Long id);
    void supplierShipOrder(Long id, Long supplierId);
    void deliverOrder(Long id);
    void markInTransit(Long id);
    void confirmReceipt(Long id);
    void approvePayout(Long id);
    Order findOrderDetail(Long id);
    Order updateContactInfo(Long id, String shippingAddress, String recipientName, String recipientPhone);
}