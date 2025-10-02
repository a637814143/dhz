package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Consumer;
import com.example.silkmall.repository.ConsumerRepository;
import com.example.silkmall.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ConsumerServiceImpl extends UserServiceImpl<Consumer> implements ConsumerService {
    private final ConsumerRepository consumerRepository;
    
    @Autowired
    public ConsumerServiceImpl(ConsumerRepository consumerRepository, PasswordEncoder passwordEncoder) {
        super(consumerRepository, passwordEncoder);
        this.consumerRepository = consumerRepository;
    }
    
    @Override
    public void updatePoints(Long consumerId, Integer points) {
        Consumer consumer = findById(consumerId)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));
        
        consumer.setPoints(consumer.getPoints() + points);
        save(consumer);
        
        // 检查是否需要升级会员等级
        upgradeMembershipLevel(consumerId);
    }
    
    @Override
    public void upgradeMembershipLevel(Long consumerId) {
        Consumer consumer = findById(consumerId)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));
        
        Integer points = consumer.getPoints();
        Integer currentLevel = consumer.getMembershipLevel() != null ? consumer.getMembershipLevel() : 1;
        
        // 根据积分决定会员等级
        if (points >= 1000 && currentLevel < 2) {
            consumer.setMembershipLevel(2);
        } else if (points >= 5000 && currentLevel < 3) {
            consumer.setMembershipLevel(3);
        } else if (points >= 10000 && currentLevel < 4) {
            consumer.setMembershipLevel(4);
        } else if (points >= 50000 && currentLevel < 5) {
            consumer.setMembershipLevel(5);
        }
        
        if (consumer.getMembershipLevel() != currentLevel) {
            save(consumer);
        }
    }
}