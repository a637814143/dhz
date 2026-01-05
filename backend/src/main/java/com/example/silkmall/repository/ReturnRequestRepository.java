package com.example.silkmall.repository;

import com.example.silkmall.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    boolean existsByOrderItemIdAndStatusIn(Long orderItemId, Collection<String> statuses);
    List<ReturnRequest> findByConsumerId(Long consumerId);
    List<ReturnRequest> findByOrderId(Long orderId);
    List<ReturnRequest> findByProduct_Supplier_Id(Long supplierId);
    List<ReturnRequest> findByAfterReceiptTrueOrderByRequestedAtDesc();
}
