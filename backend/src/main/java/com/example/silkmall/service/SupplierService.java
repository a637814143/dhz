package com.example.silkmall.service;

import com.example.silkmall.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface SupplierService extends UserService<Supplier> {
    List<Supplier> findByStatus(String status);
    List<Supplier> findBySupplierLevel(String level);
    Page<Supplier> search(String keyword, Boolean enabled, String supplierLevel, String status, Pageable pageable);
    void approveSupplier(Long id);
    void rejectSupplier(Long id, String reason);
    void updateSupplierLevel(Long id, String level);
}