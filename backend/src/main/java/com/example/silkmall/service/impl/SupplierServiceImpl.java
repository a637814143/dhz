package com.example.silkmall.service.impl;

import com.example.silkmall.dto.SupplierProfileUpdateDTO;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.repository.SupplierRepository;
import com.example.silkmall.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.Date;
import java.util.List;

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

        supplierRepository.findByEmail(email).ifPresent(existing -> {
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

        return save(supplier);
    }

    @Override
    public Supplier register(Supplier supplier) {
        if (!StringUtils.hasText(supplier.getCompanyName())) {
            throw new RuntimeException("企业名称不能为空");
        }
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
}