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

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
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
    public Page<Supplier> search(String keyword, String status, String level, Boolean enabled, Pageable pageable) {
        Specification<Supplier> specification = buildSpecification(keyword, status, level, enabled);
        return supplierRepository.findAll(specification, pageable);
    }

    private Specification<Supplier> buildSpecification(String keyword, String status, String level, Boolean enabled) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("companyName")), pattern)
                ));
            }

            if (status != null && !status.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status.trim()));
            }

            if (level != null && !level.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("supplierLevel"), level.trim()));
            }

            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}