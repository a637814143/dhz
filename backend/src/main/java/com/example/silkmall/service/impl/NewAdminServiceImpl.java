package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Admin;
import com.example.silkmall.repository.NewAdminRepository;
import com.example.silkmall.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.annotation.Primary;

@Service
@Primary
public class NewAdminServiceImpl implements AdminService {
    private final NewAdminRepository newAdminRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public NewAdminServiceImpl(NewAdminRepository newAdminRepository, PasswordEncoder passwordEncoder) {
        this.newAdminRepository = newAdminRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public Admin save(Admin admin) {
        if (admin.getId() == null) {
            throw new IllegalStateException("管理员账号不支持新建");
        }

        if (!newAdminRepository.existsById(admin.getId())) {
            throw new IllegalStateException("管理员不存在: " + admin.getId());
        }

        return newAdminRepository.save(admin);
    }
    
    @Override
    public Optional<Admin> findById(Long id) {
        return newAdminRepository.findById(id);
    }
    
    @Override
    public List<Admin> findAll() {
        return newAdminRepository.findAll();
    }
    
    @Override
    public void deleteById(Long id) {
        if (!newAdminRepository.existsById(id)) {
            throw new IllegalStateException("管理员不存在: " + id);
        }

        if (newAdminRepository.count() <= 1) {
            throw new IllegalStateException("系统必须保留至少一个管理员账号");
        }

        newAdminRepository.deleteById(id);
    }
    
    @Override
    public boolean existsById(Long id) {
        return newAdminRepository.existsById(id);
    }
    
    @Override
    public Optional<Admin> findByUsername(String username) {
        return newAdminRepository.findByUsername(username);
    }
    
    @Override
    public boolean existsByUsername(String username) {
        return newAdminRepository.existsByUsername(username);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return newAdminRepository.existsByEmail(email);
    }
    
    @Override
    public Optional<Admin> findByEmail(String email) {
        return newAdminRepository.findByEmail(email);
    }
    
    @Override
    public Admin register(Admin admin) {
        if (newAdminRepository.count() >= 1) {
            throw new IllegalStateException("系统已存在管理员账号，禁止创建新的管理员");
        }

        if (existsByUsername(admin.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (existsByEmail(admin.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }
        
        // 加密密码
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setEnabled(true);
        
        // 设置管理员默认权限
        if (admin.getPermissions() == null || admin.getPermissions().isEmpty()) {
            admin.setPermissions("BASIC");
        }
        
        return newAdminRepository.save(admin);
    }
    
    @Override
    public Admin update(Admin admin) {
        if (admin.getId() == null || !newAdminRepository.existsById(admin.getId())) {
            throw new IllegalStateException("管理员不存在: " + admin.getId());
        }

        // 确保密码不会被明文保存
        if (admin.getPassword() != null && !com.example.silkmall.security.PasswordHashValidator.isBcryptHash(admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        }
        return newAdminRepository.save(admin);
    }
    
    @Override
    public void resetPassword(Long id, String newPassword) {
        Admin admin = findById(id)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        
        admin.setPassword(passwordEncoder.encode(newPassword));
        newAdminRepository.save(admin);
    }
    
    @Override
    public void enable(Long id) {
        Admin admin = findById(id)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        
        admin.setEnabled(true);
        newAdminRepository.save(admin);
    }
    
    @Override
    public void disable(Long id) {
        Admin admin = findById(id)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
        
        admin.setEnabled(false);
        newAdminRepository.save(admin);
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
        newAdminRepository.save(admin);
    }
}
