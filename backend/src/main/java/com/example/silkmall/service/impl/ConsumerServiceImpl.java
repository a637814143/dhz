package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Consumer;
import com.example.silkmall.repository.ConsumerRepository;
import com.example.silkmall.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import jakarta.persistence.criteria.Predicate;

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
        
        int currentPoints = consumer.getPoints() != null ? consumer.getPoints() : 0;
        consumer.setPoints(currentPoints + points);
        save(consumer);

        // 检查是否需要升级会员等级
        upgradeMembershipLevel(consumerId);
    }
    
    @Override
    public void upgradeMembershipLevel(Long consumerId) {
        Consumer consumer = findById(consumerId)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));
        
        int points = consumer.getPoints() != null ? consumer.getPoints() : 0;
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

    @Override
    public Page<Consumer> search(String keyword, Boolean enabled, Integer membershipLevel, Pageable pageable) {
        Specification<Consumer> specification = buildSpecification(keyword, enabled, membershipLevel);
        return consumerRepository.findAll(specification, pageable);
    }

    private Specification<Consumer> buildSpecification(String keyword, Boolean enabled, Integer membershipLevel) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), pattern)
                ));
            }

            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }

            if (membershipLevel != null) {
                predicates.add(criteriaBuilder.equal(root.get("membershipLevel"), membershipLevel));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
