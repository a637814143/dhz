package com.example.silkmall.repository;

import com.example.silkmall.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByConsumerId(Long consumerId, Pageable pageable);
    Page<Order> findByStatus(String status, Pageable pageable);
    List<Order> findByOrderNo(String orderNo);
    List<Order> findByConsumerLookupId(String consumerLookupId);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "orderItems.product.supplier", "managingAdmin"})
    Optional<Order> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "orderItems.product.supplier"})
    Page<Order> findDistinctByOrderItems_Product_Supplier_Id(Long supplierId, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "orderItems.product.supplier", "consumer", "managingAdmin"})
    Page<Order> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "orderItems.product.supplier", "consumer", "managingAdmin"})
    Page<Order> findByConsumerConfirmationTimeIsNull(Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "orderItems.product.supplier", "consumer", "managingAdmin"})
    Page<Order> findByConsumerConfirmationTimeIsNotNull(Pageable pageable);
}