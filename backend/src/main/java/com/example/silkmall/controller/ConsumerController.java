package com.example.silkmall.controller;

import com.example.silkmall.dto.ConsumerCreateRequest;
import com.example.silkmall.dto.ConsumerDTO;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/api/consumers")
public class ConsumerController extends BaseController {
    private final ConsumerService consumerService;
    
    @Autowired
    public ConsumerController(ConsumerService consumerService) {
        this.consumerService = consumerService;
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ConsumerDTO>> listConsumers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "enabled", required = false) Boolean enabled,
            @RequestParam(value = "membershipLevel", required = false) Integer membershipLevel,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String sanitizedKeyword = keyword != null ? keyword.trim() : null;
        if (sanitizedKeyword != null && sanitizedKeyword.isEmpty()) {
            sanitizedKeyword = null;
        }
        Page<ConsumerDTO> result = consumerService.search(sanitizedKeyword, enabled, membershipLevel, pageable)
                .map(this::toDto);
        return success(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONSUMER') and #id == principal.id)")
    public ResponseEntity<?> getConsumerById(@PathVariable Long id) {
        Optional<Consumer> consumer = consumerService.findById(id);
        if (consumer.isPresent()) {
            return success(toDto(consumer.get()));
        } else {
            return notFound("消费者不存在");
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createConsumer(@Valid @RequestBody ConsumerCreateRequest request) {
        Consumer consumer = new Consumer();
        consumer.setUsername(normalize(request.getUsername()));
        consumer.setPassword(request.getPassword());
        consumer.setEmail(normalize(request.getEmail()));
        consumer.setPhone(normalize(request.getPhone()));
        consumer.setAddress(normalize(request.getAddress()));
        consumer.setAvatar(normalize(request.getAvatar()));
        consumer.setPoints(request.getPoints() != null ? request.getPoints() : 0);
        consumer.setMembershipLevel(request.getMembershipLevel() != null ? request.getMembershipLevel() : 1);
        consumer.setRole("consumer");
        consumer.setEnabled(Boolean.TRUE.equals(request.getEnabled()));

        Consumer savedConsumer = consumerService.register(consumer);

        if (Boolean.FALSE.equals(request.getEnabled())) {
            consumerService.disable(savedConsumer.getId());
            savedConsumer = consumerService.findById(savedConsumer.getId()).orElse(savedConsumer);
        }

        return created(toDto(savedConsumer));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONSUMER') and #id == principal.id)")
    public ResponseEntity<?> updateConsumer(@PathVariable Long id, @Valid @RequestBody ConsumerDTO consumerDTO) {
        // 检查消费者是否存在
        Consumer existingConsumer = consumerService.findById(id)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));

        // 更新消费者信息
        applyDtoToEntity(consumerDTO, existingConsumer);

        Consumer saved = consumerService.save(existingConsumer);
        return success(toDto(saved));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteConsumer(@PathVariable Long id) {
        consumerService.deleteById(id);
        return success();
    }
    
    @PostMapping("/register")
    public ResponseEntity<Consumer> register(@RequestBody Consumer consumer) {
        return created(consumerService.register(consumer));
    }
    
    @PutMapping("/{id}/points")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updatePoints(@PathVariable Long id, @RequestParam Integer points) {
        consumerService.updatePoints(id, points);
        return success();
    }
    
    @PostMapping("/{id}/upgrade")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> upgradeMembershipLevel(@PathVariable Long id) {
        consumerService.upgradeMembershipLevel(id);
        return success();
    }

    private ConsumerDTO toDto(Consumer consumer) {
        ConsumerDTO dto = new ConsumerDTO();
        dto.setId(consumer.getId());
        dto.setUsername(consumer.getUsername());
        dto.setEmail(consumer.getEmail());
        dto.setPhone(consumer.getPhone());
        dto.setAddress(consumer.getAddress());
        dto.setAvatar(consumer.getAvatar());
        dto.setPoints(consumer.getPoints());
        dto.setMembershipLevel(consumer.getMembershipLevel());
        dto.setEnabled(consumer.isEnabled());
        return dto;
    }

    private void applyDtoToEntity(ConsumerDTO dto, Consumer consumer) {
        consumer.setUsername(normalize(dto.getUsername()));
        consumer.setEmail(normalize(dto.getEmail()));
        consumer.setPhone(normalize(dto.getPhone()));
        consumer.setAddress(normalize(dto.getAddress()));
        consumer.setAvatar(normalize(dto.getAvatar()));
        if (dto.getPoints() != null) {
            consumer.setPoints(dto.getPoints());
        }
        if (dto.getMembershipLevel() != null) {
            consumer.setMembershipLevel(dto.getMembershipLevel());
        }
        if (dto.getEnabled() != null) {
            consumer.setEnabled(dto.getEnabled());
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
