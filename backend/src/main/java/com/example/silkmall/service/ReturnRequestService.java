package com.example.silkmall.service;

import com.example.silkmall.entity.ReturnRequest;
import com.example.silkmall.security.CustomUserDetails;

import java.util.List;

public interface ReturnRequestService extends BaseService<ReturnRequest, Long> {
    ReturnRequest createReturnRequest(Long orderItemId, ReturnRequest request);
    ReturnRequest processReturnRequest(Long id, String status, String resolution, CustomUserDetails actor);
    List<ReturnRequest> findByConsumerId(Long consumerId);
    List<ReturnRequest> findByOrderId(Long orderId);
}
