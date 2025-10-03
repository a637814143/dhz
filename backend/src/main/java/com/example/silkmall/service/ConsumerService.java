package com.example.silkmall.service;

import com.example.silkmall.entity.Consumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConsumerService extends UserService<Consumer> {
    // 可以添加特定于消费者的服务方法
    void updatePoints(Long consumerId, Integer points);
    void upgradeMembershipLevel(Long consumerId);

    Page<Consumer> search(String keyword, Boolean enabled, Integer membershipLevel, Pageable pageable);
}
