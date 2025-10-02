package com.example.silkmall.controller;

import com.example.silkmall.entity.Supplier;
import com.example.silkmall.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierController extends BaseController {
    private final SupplierService supplierService;
    
    @Autowired
    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPPLIER') and #id == principal.id)")
    public ResponseEntity<?> getSupplierById(@PathVariable Long id) {
        Optional<Supplier> supplier = supplierService.findById(id);
        if (supplier.isPresent()) {
            return success(supplier.get());
        } else {
            return notFound("供应商不存在");
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SUPPLIER') and #id == principal.id)")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        supplier.setId(id);
        return success(supplierService.update(supplier));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteById(id);
        return success();
    }
    
    @PostMapping("/register")
    public ResponseEntity<Supplier> register(@RequestBody Supplier supplier) {
        return created(supplierService.register(supplier));
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Supplier>> getSuppliersByStatus(@PathVariable String status) {
        return success(supplierService.findByStatus(status));
    }
    
    @GetMapping("/level/{level}")
    public ResponseEntity<List<Supplier>> getSuppliersByLevel(@PathVariable String level, Pageable pageable) {
        return success(supplierService.findBySupplierLevel(level));
    }
    
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> approveSupplier(@PathVariable Long id) {
        supplierService.approveSupplier(id);
        return success();
    }
    
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> rejectSupplier(@PathVariable Long id, @RequestParam String reason) {
        supplierService.rejectSupplier(id, reason);
        return success();
    }
    
    @PutMapping("/{id}/level")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateSupplierLevel(@PathVariable Long id, @RequestParam String level) {
        supplierService.updateSupplierLevel(id, level);
        return success();
    }
}