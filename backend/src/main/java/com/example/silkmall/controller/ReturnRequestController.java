package com.example.silkmall.controller;

import com.example.silkmall.dto.CreateReturnRequestDTO;
import com.example.silkmall.dto.ProcessReturnRequestDTO;
import com.example.silkmall.dto.ReturnRequestDTO;
import com.example.silkmall.entity.ReturnRequest;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.service.ReturnRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/returns")
public class ReturnRequestController extends BaseController {
    private final ReturnRequestService returnRequestService;

    @Autowired
    public ReturnRequestController(ReturnRequestService returnRequestService) {
        this.returnRequestService = returnRequestService;
    }

    @PostMapping("/order-items/{orderItemId}")
    @PreAuthorize("hasAnyRole('CONSUMER', 'ADMIN')")
    public ResponseEntity<ReturnRequestDTO> createReturnRequest(@PathVariable Long orderItemId,
                                                                 @RequestBody CreateReturnRequestDTO request) {
        ReturnRequest entity = new ReturnRequest();
        entity.setReason(request.getReason());

        ReturnRequest saved = returnRequestService.createReturnRequest(orderItemId, entity);
        return created(toDto(saved));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPPLIER', 'ADMIN')")
    public ResponseEntity<ReturnRequestDTO> processReturn(@PathVariable Long id,
                                                           @RequestBody ProcessReturnRequestDTO request,
                                                           @AuthenticationPrincipal CustomUserDetails currentUser) {
        ReturnRequest updated = returnRequestService.processReturnRequest(id, request.getStatus(), request.getResolution(), currentUser);
        return success(toDto(updated));
    }

    @GetMapping("/consumers/{consumerId}")
    public ResponseEntity<List<ReturnRequestDTO>> getByConsumer(@PathVariable Long consumerId) {
        List<ReturnRequestDTO> requests = returnRequestService.findByConsumerId(consumerId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return success(requests);
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<List<ReturnRequestDTO>> getByOrder(@PathVariable Long orderId) {
        List<ReturnRequestDTO> requests = returnRequestService.findByOrderId(orderId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return success(requests);
    }

    @GetMapping("/suppliers/{supplierId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPPLIER') and #supplierId == principal.id)")
    public ResponseEntity<List<ReturnRequestDTO>> getBySupplier(@PathVariable Long supplierId) {
        List<ReturnRequestDTO> requests = returnRequestService.findBySupplierId(supplierId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return success(requests);
    }

    private ReturnRequestDTO toDto(ReturnRequest request) {
        return ReturnRequestDTO.builder()
                .id(request.getId())
                .orderId(request.getOrder().getId())
                .orderItemId(request.getOrderItem().getId())
                .productId(request.getProduct().getId())
                .productName(request.getProduct().getName())
                .consumerId(request.getConsumer().getId())
                .consumerName(request.getConsumer().getUsername())
                .status(request.getStatus())
                .reason(request.getReason())
                .resolution(request.getResolution())
                .requestedAt(request.getRequestedAt())
                .processedAt(request.getProcessedAt())
                .build();
    }
}
