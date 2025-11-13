package com.example.silkmall.repository;

import com.example.silkmall.entity.User;
import java.util.Optional;

public interface BaseUserRepository<T extends User> {
    Optional<T> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<T> findByEmail(String email);
}