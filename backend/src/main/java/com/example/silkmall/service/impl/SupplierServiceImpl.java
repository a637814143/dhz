package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Supplier;
import com.example.silkmall.repository.SupplierRepository;
import com.example.silkmall.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
public class SupplierServiceImpl extends UserServiceImpl<Supplier> implements SupplierService {
    private final SupplierRepository supplierRepository;
    
    @Autowired
    public SupplierServiceImpl(SupplierRepository supplierRepository, PasswordEncoder passwordEncoder) {
        super(supplierRepository, passwordEncoder);
        this.supplierRepository = supplierRepository;
    }
    
    @Override
    public List<Supplier> findByStatus(String status) {
        return supplierRepository.findByStatus(status);
    }
    
    @Override
    public List<Supplier> findBySupplierLevel(String level) {
        return supplierRepository.findBySupplierLevel(level);
    }
    
    @Override
    public void approveSupplier(Long id) {
        Supplier supplier = findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));
        
        supplier.setStatus("APPROVED");
        supplier.setEnabled(true);
        save(supplier);
    }
    
    @Override
    public void rejectSupplier(Long id, String reason) {
        Supplier supplier = findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));
        
        supplier.setStatus("REJECTED");
        supplier.setEnabled(false);
        save(supplier);
    }
    
    @Override
    public void updateSupplierLevel(Long id, String level) {
        Supplier supplier = findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));

        supplier.setSupplierLevel(level);
        save(supplier);
    }

    @Override
    public Page<Supplier> searchSuppliers(String keyword, String status, String level, Boolean enabled, Pageable pageable) {
        Specification<Supplier> specification = buildSpecification(keyword, status, level, enabled);
        return supplierRepository.findAll(specification, pageable);
    }

    @Override
    public Supplier register(Supplier supplier) {
        // 设置默认状态为待审核
        if (supplier.getStatus() == null) {
            supplier.setStatus("PENDING");
            supplier.setEnabled(false);
        }

        // 设置默认供应商等级
        if (supplier.getSupplierLevel() == null) {
            supplier.setSupplierLevel("BRONZE");
        }

        // 设置注册日期
        if (supplier.getJoinDate() == null) {
            supplier.setJoinDate(new Date());
        }

        return super.register(supplier);
    }

    @Override
    public Supplier update(Supplier supplier) {
        if (supplier.getId() == null) {
            throw new RuntimeException("供应商不存在");
        }

        Supplier existing = findById(supplier.getId())
                .orElseThrow(() -> new RuntimeException("供应商不存在"));

        if (supplier.getUsername() != null) {
            existing.setUsername(supplier.getUsername());
        }
        if (supplier.getEmail() != null) {
            existing.setEmail(supplier.getEmail());
        }
        if (supplier.getPhone() != null) {
            existing.setPhone(supplier.getPhone());
        }
        if (supplier.getAddress() != null) {
            existing.setAddress(supplier.getAddress());
        }
        if (supplier.getCompanyName() != null) {
            existing.setCompanyName(supplier.getCompanyName());
        }
        if (supplier.getBusinessLicense() != null) {
            existing.setBusinessLicense(supplier.getBusinessLicense());
        }
        if (supplier.getContactPerson() != null) {
            existing.setContactPerson(supplier.getContactPerson());
        }
        if (supplier.getJoinDate() != null) {
            existing.setJoinDate(supplier.getJoinDate());
        }
        if (supplier.getSupplierLevel() != null) {
            existing.setSupplierLevel(supplier.getSupplierLevel());
        }
        if (supplier.getStatus() != null) {
            existing.setStatus(supplier.getStatus());
        }
        existing.setEnabled(supplier.isEnabled());
        if (supplier.getWalletBalance() != null) {
            existing.setWalletBalance(supplier.getWalletBalance());
        }
        if (supplier.getPassword() != null && !supplier.getPassword().isBlank()) {
            existing.setPassword(supplier.getPassword());
        }

        return super.update(existing);
    }
    private Specification<Supplier> buildSpecification(String keyword, String status, String level, Boolean enabled) {
        Specification<Supplier> specification = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("companyName")), pattern),
                    cb.like(cb.lower(root.get("contactPerson")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("phone")), pattern)
            ));
        }

        if (status != null && !status.isBlank()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("status"), status.trim().toUpperCase(Locale.ROOT)));
        }

        if (level != null && !level.isBlank()) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("supplierLevel"), level.trim().toUpperCase(Locale.ROOT)));
        }

        if (enabled != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("enabled"), enabled));
        }

        return specification;
    }
}