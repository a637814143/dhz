package com.example.silkmall.service.impl;

import com.example.silkmall.entity.User;
import com.example.silkmall.repository.BaseUserRepository;
import com.example.silkmall.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Optional;

public abstract class UserServiceImpl<T extends User> implements UserService<T> {
    protected final BaseUserRepository<T> userRepository;
    protected final PasswordEncoder passwordEncoder;
    protected final JpaRepository<T, Long> jpaRepository;
    
    @Autowired
    protected UserServiceImpl(BaseUserRepository<T> userRepository, 
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jpaRepository = (JpaRepository<T, Long>) userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Optional<T> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public Optional<T> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    
    @Override
    public T register(T user) {
        if (existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(true);
        
        return jpaRepository.save(user);
    }

    @Override
    public T update(T user) {
        // 确保密码不会被明文保存
        if (user.getPassword() != null && !user.getPassword().startsWith("{bcrypt}")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return jpaRepository.save(user);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        T user = findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        user.setPassword(passwordEncoder.encode(newPassword));
        jpaRepository.save(user);
    }
    
    @Override
    public void enable(Long id) {
        T user = findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        user.setEnabled(true);
        jpaRepository.save(user);
    }
    
    @Override
    public void disable(Long id) {
        T user = findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        
        user.setEnabled(false);
        jpaRepository.save(user);
    }

    @Override
    public T save(T entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public Optional<T> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<T> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}