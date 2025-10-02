package com.example.silkmall.controller;

import com.example.silkmall.dto.ConsumerDTO;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/consumers")
public class ConsumerController extends BaseController {
    private final ConsumerService consumerService;
    
    @Autowired
    public ConsumerController(ConsumerService consumerService) {
        this.consumerService = consumerService;
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONSUMER') and #id == principal.id)")
    public ResponseEntity<?> getConsumerById(@PathVariable Long id) {
        Optional<Consumer> consumer = consumerService.findById(id);
        if (consumer.isPresent()) {
            return success(consumer.get());
        } else {
            return notFound("消费者不存在");
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('CONSUMER') and #id == principal.id)")
    public ResponseEntity<?> updateConsumer(@PathVariable Long id, @RequestBody ConsumerDTO consumerDTO) {
        // 检查消费者是否存在
        Consumer existingConsumer = consumerService.findById(id)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));
        
        // 更新消费者信息
        existingConsumer.setUsername(consumerDTO.getUsername());
        existingConsumer.setEmail(consumerDTO.getEmail());
        existingConsumer.setPhone(consumerDTO.getPhone());
        existingConsumer.setAddress(consumerDTO.getAddress());
        existingConsumer.setRealName(consumerDTO.getRealName());
        existingConsumer.setIdCard(consumerDTO.getIdCard());
        existingConsumer.setAvatar(consumerDTO.getAvatar());
        existingConsumer.setPoints(consumerDTO.getPoints());
        existingConsumer.setMembershipLevel(consumerDTO.getMembershipLevel());
        if (consumerDTO.getEnabled() != null) {
            existingConsumer.setEnabled(consumerDTO.getEnabled());
        }
        
        return success(consumerService.save(existingConsumer));
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
}