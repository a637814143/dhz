package com.example.silkmall.service;

import com.example.silkmall.entity.Supplier;
import com.example.silkmall.dto.SupplierProfileUpdateDTO;
import java.util.List;

public interface SupplierService extends UserService<Supplier> {
    List<Supplier> findByStatus(String status);
    List<Supplier> findBySupplierLevel(String level);
    void approveSupplier(Long id);
    void rejectSupplier(Long id, String reason);
    void updateSupplierLevel(Long id, String level);
    Supplier updateProfile(Long id, SupplierProfileUpdateDTO updateDTO);
}