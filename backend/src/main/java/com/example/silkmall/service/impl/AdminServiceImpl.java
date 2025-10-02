package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Admin;
import com.example.silkmall.repository.AdminRepository;
import com.example.silkmall.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class AdminServiceImpl extends UserServiceImpl<Admin> implements AdminService {
    private final AdminRepository adminRepository;
    
    @Autowired
    public AdminServiceImpl(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        super(adminRepository, passwordEncoder);
        this.adminRepository = adminRepository;
    }
    
    @Override
    public boolean hasPermission(Long adminId, String permission) {
        Admin admin = findById(adminId)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        
        if (admin.getPermissions() == null || admin.getPermissions().isEmpty()) {
            return false;
        }
        
        Set<String> permissionSet = new HashSet<>(Arrays.asList(admin.getPermissions().split(",")));
        return permissionSet.contains(permission) || permissionSet.contains("ALL");
    }
    
    @Override
    public void updatePermissions(Long id, String permissions) {
        Admin admin = findById(id)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        
        admin.setPermissions(permissions);
        save(admin);
    }
    
    @Override
    public Admin register(Admin admin) {
        // 设置管理员默认权限
        if (admin.getPermissions() == null || admin.getPermissions().isEmpty()) {
            admin.setPermissions("BASIC");
        }
        
        return super.register(admin);
    }
}