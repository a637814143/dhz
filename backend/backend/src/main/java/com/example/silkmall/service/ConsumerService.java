package com.example.silkmall.service;

import com.example.silkmall.entity.Consumer;

public interface ConsumerService extends UserService<Consumer> {
    // 可以添加特定于消费者的服务方法
    void updatePoints(Long consumerId, Integer points);
    void upgradeMembershipLevel(Long consumerId);
}