package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Supplier;
import com.example.silkmall.repository.NewSupplierRepository;
import com.example.silkmall.service.SupplierService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Primary
public class NewSupplierServiceImpl implements SupplierService {
    private final NewSupplierRepository newSupplierRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public NewSupplierServiceImpl(NewSupplierRepository newSupplierRepository, PasswordEncoder passwordEncoder) {
        this.newSupplierRepository = newSupplierRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public Supplier save(Supplier supplier) {
        return newSupplierRepository.save(supplier);
    }
    
    @Override
    public Optional<Supplier> findById(Long id) {
        return newSupplierRepository.findById(id);
    }
    
    @Override
    public List<Supplier> findAll() {
        return newSupplierRepository.findAll();
    }
    
    @Override
    public void deleteById(Long id) {
        newSupplierRepository.deleteById(id);
    }
    
    @Override
    public boolean existsById(Long id) {
        return newSupplierRepository.existsById(id);
    }
    
    @Override
    public Optional<Supplier> findByUsername(String username) {
        return newSupplierRepository.findByUsername(username);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return newSupplierRepository.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return newSupplierRepository.existsByEmail(email);
    }
    
    @Override
    public Optional<Supplier> findByEmail(String email) {
        return newSupplierRepository.findByEmail(email);
    }
    
    @Override
    public List<Supplier> findByStatus(String status) {
        return newSupplierRepository.findByStatus(status);
    }
    
    @Override
    public List<Supplier> findBySupplierLevel(String level) {
        return newSupplierRepository.findBySupplierLevel(level);
    }

    @Override
    public Page<Supplier> search(String keyword, Boolean enabled, String supplierLevel, String status,
                                 Pageable pageable) {
        Specification<Supplier> specification = buildSpecification(keyword, enabled, supplierLevel, status);
        return newSupplierRepository.findAll(specification, pageable);
    }
    
    @Override
    public Supplier register(Supplier supplier) {
        if (existsByUsername(supplier.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (existsByEmail(supplier.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }
        
        // 加密密码
        supplier.setPassword(passwordEncoder.encode(supplier.getPassword()));
        supplier.setEnabled(true);
        
        // 新供应商默认状态为待审核
        if (supplier.getStatus() == null || supplier.getStatus().isEmpty()) {
            supplier.setStatus("PENDING");
        }
        
        // 默认供应商等级
        if (supplier.getSupplierLevel() == null || supplier.getSupplierLevel().isEmpty()) {
            supplier.setSupplierLevel("BRONZE");
        }
        
        return newSupplierRepository.save(supplier);
    }
    
    @Override
    public Supplier update(Supplier supplier) {
        // 确保密码不会被明文保存
        if (supplier.getPassword() != null && !com.example.silkmall.security.PasswordHashValidator.isBcryptHash(supplier.getPassword())) {
            supplier.setPassword(passwordEncoder.encode(supplier.getPassword()));
        }
        return newSupplierRepository.save(supplier);
    }
    
    @Override
    public void resetPassword(Long id, String newPassword) {
        Supplier supplier = findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));
        
        supplier.setPassword(passwordEncoder.encode(newPassword));
        newSupplierRepository.save(supplier);
    }
    
    @Override
    public void enable(Long id) {
        Supplier supplier = findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));
        
        supplier.setEnabled(true);
        newSupplierRepository.save(supplier);
    }
    
    @Override
    public void disable(Long id) {
        Supplier supplier = findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));
        
        supplier.setEnabled(false);
        newSupplierRepository.save(supplier);
    }
    
    @Override
    public void approveSupplier(Long id) {
        Supplier supplier = findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));
        
        supplier.setStatus("APPROVED");
        newSupplierRepository.save(supplier);
    }
    
    @Override
    public void rejectSupplier(Long id, String reason) {
        Supplier supplier = findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));
        
        supplier.setStatus("REJECTED");
        // 假设Supplier实体有一个reason字段
        // supplier.setRejectionReason(reason);
        newSupplierRepository.save(supplier);
    }
    
    @Override
    public void updateSupplierLevel(Long id, String level) {
        Supplier supplier = findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));

        supplier.setSupplierLevel(level);
        newSupplierRepository.save(supplier);
    }

    private Specification<Supplier> buildSpecification(String keyword, Boolean enabled, String supplierLevel,
                                                       String status) {
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

            if (enabled != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), enabled));
            }

            if (supplierLevel != null && !supplierLevel.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("supplierLevel"), supplierLevel));
            }

            if (status != null && !status.isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
