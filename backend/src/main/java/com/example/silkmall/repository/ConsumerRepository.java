package com.example.silkmall.repository;

import com.example.silkmall.entity.Consumer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsumerRepository extends JpaRepository<Consumer, Long>,
        JpaSpecificationExecutor<Consumer>, BaseUserRepository<Consumer> {
}
