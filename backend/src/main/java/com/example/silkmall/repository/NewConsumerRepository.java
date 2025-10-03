package com.example.silkmall.repository;

import com.example.silkmall.entity.Consumer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NewConsumerRepository extends JpaRepository<Consumer, Long>,
        JpaSpecificationExecutor<Consumer> {
    Optional<Consumer> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<Consumer> findByEmail(String email);
}
