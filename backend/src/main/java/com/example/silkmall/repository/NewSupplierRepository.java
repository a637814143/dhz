package com.example.silkmall.repository;

import com.example.silkmall.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewSupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {
    Optional<Supplier> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<Supplier> findByEmail(String email);
    List<Supplier> findByStatus(String status);
    List<Supplier> findBySupplierLevel(String level);
}