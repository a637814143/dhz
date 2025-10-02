package com.example.silkmall.service;

import com.example.silkmall.entity.User;
import java.util.Optional;

public interface UserService<T extends User> extends BaseService<T, Long> {
    Optional<T> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<T> findByEmail(String email);
    T register(T user);
    T update(T user);
    void resetPassword(Long id, String newPassword);
    void enable(Long id);
    void disable(Long id);
}