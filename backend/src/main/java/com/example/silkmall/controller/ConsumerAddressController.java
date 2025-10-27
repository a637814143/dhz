package com.example.silkmall.controller;

import com.example.silkmall.dto.ConsumerAddressDTO;
import com.example.silkmall.dto.ConsumerAddressRequest;
import com.example.silkmall.entity.ConsumerAddress;
import com.example.silkmall.service.ConsumerAddressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/consumers/{consumerId}/addresses")
public class ConsumerAddressController extends BaseController {
    private final ConsumerAddressService consumerAddressService;

    public ConsumerAddressController(ConsumerAddressService consumerAddressService) {
        this.consumerAddressService = consumerAddressService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONSUMER') and #consumerId == principal.id)")
    public ResponseEntity<List<ConsumerAddressDTO>> list(@PathVariable Long consumerId) {
        List<ConsumerAddressDTO> result = consumerAddressService.listAddresses(consumerId)
                .stream()
                .map(this::toDto)
                .toList();
        return success(result);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONSUMER') and #consumerId == principal.id)")
    public ResponseEntity<?> create(@PathVariable Long consumerId, @Valid @RequestBody ConsumerAddressRequest request) {
        try {
            ConsumerAddress payload = fromRequest(request);
            ConsumerAddress saved = consumerAddressService.createAddress(consumerId, payload);
            return created(toDto(saved));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        }
    }

    @PutMapping("/{addressId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONSUMER') and #consumerId == principal.id)")
    public ResponseEntity<?> update(
            @PathVariable Long consumerId,
            @PathVariable Long addressId,
            @Valid @RequestBody ConsumerAddressRequest request
    ) {
        try {
            ConsumerAddress payload = fromRequest(request);
            ConsumerAddress updated = consumerAddressService.updateAddress(consumerId, addressId, payload);
            return success(toDto(updated));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        }
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONSUMER') and #consumerId == principal.id)")
    public ResponseEntity<?> delete(@PathVariable Long consumerId, @PathVariable Long addressId) {
        try {
            consumerAddressService.deleteAddress(consumerId, addressId);
            return success();
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        }
    }

    @PostMapping("/{addressId}/default")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONSUMER') and #consumerId == principal.id)")
    public ResponseEntity<?> markDefault(@PathVariable Long consumerId, @PathVariable Long addressId) {
        try {
            ConsumerAddress updated = consumerAddressService.markDefault(consumerId, addressId);
            return success(toDto(updated));
        } catch (IllegalArgumentException ex) {
            return badRequest(ex.getMessage());
        }
    }

    private ConsumerAddress fromRequest(ConsumerAddressRequest request) {
        ConsumerAddress address = new ConsumerAddress();
        address.setRecipientName(normalize(request.getRecipientName()));
        address.setRecipientPhone(normalize(request.getRecipientPhone()));
        address.setShippingAddress(normalize(request.getShippingAddress()));
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        return address;
    }

    private ConsumerAddressDTO toDto(ConsumerAddress address) {
        ConsumerAddressDTO dto = new ConsumerAddressDTO();
        dto.setId(address.getId());
        dto.setConsumerId(address.getConsumer() != null ? address.getConsumer().getId() : null);
        dto.setRecipientName(address.getRecipientName());
        dto.setRecipientPhone(address.getRecipientPhone());
        dto.setShippingAddress(address.getShippingAddress());
        dto.setIsDefault(address.getIsDefault());
        dto.setCreatedAt(address.getCreatedAt());
        dto.setUpdatedAt(address.getUpdatedAt());
        return dto;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
