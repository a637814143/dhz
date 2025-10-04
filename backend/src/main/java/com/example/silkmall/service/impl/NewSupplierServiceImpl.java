package com.example.silkmall.service.impl;

import com.example.silkmall.dto.SupplierProfileUpdateDTO;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.repository.NewSupplierRepository;
import com.example.silkmall.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

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
    public Supplier register(Supplier supplier) {
        if (existsByUsername(supplier.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (existsByEmail(supplier.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }

        if (!StringUtils.hasText(supplier.getCompanyName())) {
            throw new RuntimeException("企业名称不能为空");
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
        if (supplier.getPassword() != null && !supplier.getPassword().startsWith("{bcrypt}")) {
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

    @Override
    public Supplier updateProfile(Long id, SupplierProfileUpdateDTO updateDTO) {
        Supplier supplier = findById(id)
                .orElseThrow(() -> new RuntimeException("供应商不存在"));

        String companyName = updateDTO.getCompanyName() != null ? updateDTO.getCompanyName().trim() : "";
        if (!StringUtils.hasText(companyName)) {
            throw new RuntimeException("企业名称不能为空");
        }

        String email = updateDTO.getEmail() != null ? updateDTO.getEmail().trim() : "";
        if (!StringUtils.hasText(email)) {
            throw new RuntimeException("邮箱不能为空");
        }

        newSupplierRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("邮箱已被其他账户使用");
            }
        });

        String phone = updateDTO.getPhone() != null ? updateDTO.getPhone().trim() : "";
        if (!StringUtils.hasText(phone)) {
            throw new RuntimeException("联系电话不能为空");
        }

        supplier.setCompanyName(companyName);
        supplier.setEmail(email);
        supplier.setPhone(phone);
        supplier.setContactPerson(
                StringUtils.hasText(updateDTO.getContactPerson())
                        ? updateDTO.getContactPerson().trim()
                        : null
        );
        supplier.setBusinessLicense(
                StringUtils.hasText(updateDTO.getBusinessLicense())
                        ? updateDTO.getBusinessLicense().trim()
                        : null
        );

        return newSupplierRepository.save(supplier);
    }
}