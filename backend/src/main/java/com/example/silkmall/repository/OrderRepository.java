package com.example.silkmall.repository;

import com.example.silkmall.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByConsumerId(Long consumerId, Pageable pageable);
    Page<Order> findByStatus(String status, Pageable pageable);
    List<Order> findByOrderNo(String orderNo);
    List<Order> findByConsumerLookupId(String consumerLookupId);
}