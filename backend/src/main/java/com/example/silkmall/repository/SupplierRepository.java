package com.example.silkmall.repository;

import com.example.silkmall.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long>, BaseUserRepository<Supplier> {
    List<Supplier> findByStatus(String status);
    List<Supplier> findBySupplierLevel(String level);
}