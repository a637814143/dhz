package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Consumer;
import com.example.silkmall.repository.NewConsumerRepository;
import com.example.silkmall.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.context.annotation.Primary;

@Service
@Primary
public class NewConsumerServiceImpl implements ConsumerService {
    private final NewConsumerRepository newConsumerRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public NewConsumerServiceImpl(NewConsumerRepository newConsumerRepository, PasswordEncoder passwordEncoder) {
        this.newConsumerRepository = newConsumerRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public Consumer save(Consumer consumer) {
        return newConsumerRepository.save(consumer);
    }
    
    @Override
    public Optional<Consumer> findById(Long id) {
        return newConsumerRepository.findById(id);
    }
    
    @Override
    public List<Consumer> findAll() {
        return newConsumerRepository.findAll();
    }
    
    @Override
    public void deleteById(Long id) {
        newConsumerRepository.deleteById(id);
    }
    
    @Override
    public boolean existsById(Long id) {
        return newConsumerRepository.existsById(id);
    }
    
    @Override
    public Optional<Consumer> findByUsername(String username) {
        return newConsumerRepository.findByUsername(username);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return newConsumerRepository.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return newConsumerRepository.existsByEmail(email);
    }
    
    @Override
    public Optional<Consumer> findByEmail(String email) {
        return newConsumerRepository.findByEmail(email);
    }
    
    @Override
    public Consumer register(Consumer consumer) {
        if (existsByUsername(consumer.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (existsByEmail(consumer.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }
        
        // 加密密码
        consumer.setPassword(passwordEncoder.encode(consumer.getPassword()));
        consumer.setEnabled(true);
        
        return newConsumerRepository.save(consumer);
    }
    
    @Override
    public Consumer update(Consumer consumer) {
        if (consumer.getId() == null) {
            throw new IllegalArgumentException("更新消费者信息时必须提供ID");
        }
        Consumer existingConsumer = newConsumerRepository.findById(consumer.getId())
                .orElseThrow(() -> new RuntimeException("消费者不存在"));

        if (consumer.getUsername() != null) {
            existingConsumer.setUsername(consumer.getUsername());
        }
        if (consumer.getEmail() != null) {
            existingConsumer.setEmail(consumer.getEmail());
        }
        if (consumer.getPhone() != null) {
            existingConsumer.setPhone(consumer.getPhone());
        }
        if (consumer.getAddress() != null) {
            existingConsumer.setAddress(consumer.getAddress());
        }

        if (consumer.getRealName() != null) {
            existingConsumer.setRealName(consumer.getRealName());
        }
        if (consumer.getIdCard() != null) {
            existingConsumer.setIdCard(consumer.getIdCard());
        }
        if (consumer.getAvatar() != null) {
            existingConsumer.setAvatar(consumer.getAvatar());
        }
        if (consumer.getPoints() != null) {
            existingConsumer.setPoints(consumer.getPoints());
        }
        if (consumer.getMembershipLevel() != null) {
            existingConsumer.setMembershipLevel(consumer.getMembershipLevel());
        }

        if (consumer.getPassword() != null && !consumer.getPassword().isEmpty()) {
            String password = consumer.getPassword();
            if (!isPasswordEncoded(password)) {
                password = passwordEncoder.encode(password);
            }
            existingConsumer.setPassword(password);
        }

        return newConsumerRepository.save(existingConsumer);
    }
    
    @Override
    public void resetPassword(Long id, String newPassword) {
        Consumer consumer = findById(id)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));
        
        consumer.setPassword(passwordEncoder.encode(newPassword));
        newConsumerRepository.save(consumer);
    }
    
    @Override
    public void enable(Long id) {
        Consumer consumer = findById(id)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));
        
        consumer.setEnabled(true);
        newConsumerRepository.save(consumer);
    }
    
    @Override
    public void disable(Long id) {
        Consumer consumer = findById(id)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));
        
        consumer.setEnabled(false);
        newConsumerRepository.save(consumer);
    }
    
    @Override
    public void updatePoints(Long consumerId, Integer points) {
        Consumer consumer = findById(consumerId)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));

        int currentPoints = consumer.getPoints() != null ? consumer.getPoints() : 0;
        consumer.setPoints(currentPoints + points);
        newConsumerRepository.save(consumer);

        // 检查是否需要升级会员等级
        upgradeMembershipLevel(consumerId);
    }
    
    @Override
    public void upgradeMembershipLevel(Long consumerId) {
        Consumer consumer = findById(consumerId)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));
        
        int points = consumer.getPoints() != null ? consumer.getPoints() : 0;
        Integer currentLevel = consumer.getMembershipLevel() != null ? consumer.getMembershipLevel() : 1;
        
        // 根据积分升级会员等级
        if (points >= 10000 && currentLevel < 5) {
            consumer.setMembershipLevel(5); // VIP
        } else if (points >= 5000 && currentLevel < 4) {
            consumer.setMembershipLevel(4); // GOLD
        } else if (points >= 2000 && currentLevel < 3) {
            consumer.setMembershipLevel(3); // SILVER
        }

        newConsumerRepository.save(consumer);
    }

    @Override
    public Page<Consumer> search(String keyword, Boolean enabled, Integer membershipLevel, Pageable pageable) {
        Specification<Consumer> specification = buildSpecification(keyword, enabled, membershipLevel);
        return newConsumerRepository.findAll(specification, pageable);
    }

    private Specification<Consumer> buildSpecification(String keyword, Boolean enabled, Integer membershipLevel) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("realName")), pattern)
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
    private boolean isPasswordEncoded(String password) {
        return password.startsWith("{bcrypt}")
                || password.startsWith("$2a$")
                || password.startsWith("$2b$")
                || password.startsWith("$2y$");
    }
}
