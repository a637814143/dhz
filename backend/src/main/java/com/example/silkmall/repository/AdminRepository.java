package com.example.silkmall.repository;

import com.example.silkmall.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long>, BaseUserRepository<Admin> {
    Optional<Admin> findTopByOrderByIdAsc();
}