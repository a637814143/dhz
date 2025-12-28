package com.example.silkmall.security;

import com.example.silkmall.entity.Admin;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.service.impl.NewAdminServiceImpl;
import com.example.silkmall.service.impl.NewConsumerServiceImpl;
import com.example.silkmall.service.impl.NewSupplierServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    private final NewConsumerServiceImpl consumerService;
    private final NewSupplierServiceImpl supplierService;
    private final NewAdminServiceImpl adminService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public CustomUserDetailsService(
            @Lazy NewConsumerServiceImpl consumerService,
            @Lazy NewSupplierServiceImpl supplierService,
            @Lazy NewAdminServiceImpl adminService,
            PasswordEncoder passwordEncoder
    ) {
        this.consumerService = consumerService;
        this.supplierService = supplierService;
        this.adminService = adminService;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 首先尝试查找消费者
        Consumer consumer = consumerService.findByUsername(username)
                .orElse(null);
        
        if (consumer != null) {
            ensurePasswordEncoded(consumer);
            return new CustomUserDetails(
                    consumer.getId(),
                    consumer.getUsername(),
                    consumer.getPassword(),
                    consumer.getEmail(),
                    consumer.getPhone(),
                    "consumer",
                    consumer.isEnabled(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + consumer.getRole().toUpperCase()))
            );
        }
        
        // 然后尝试查找供应商
        Supplier supplier = supplierService.findByUsername(username)
                .orElse(null);
        
        if (supplier != null) {
            ensurePasswordEncoded(supplier);
            return new CustomUserDetails(
                    supplier.getId(),
                    supplier.getUsername(),
                    supplier.getPassword(),
                    supplier.getEmail(),
                    supplier.getPhone(),
                    "supplier",
                    supplier.isEnabled(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + supplier.getRole().toUpperCase()))
            );
        }
        
        // 最后尝试查找管理员
        Admin admin = adminService.findByUsername(username)
                .orElse(null);
        
        if (admin != null) {
            ensurePasswordEncoded(admin);
            ensureAdminDefaults(admin);
            return new CustomUserDetails(
                    admin.getId(),
                    admin.getUsername(),
                    admin.getPassword(),
                    admin.getEmail(),
                    admin.getPhone(),
                    "admin",
                    admin.isEnabled(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + admin.getRole().toUpperCase()))
            );
        }
        
        throw new UsernameNotFoundException("用户不存在: " + username);
    }
    
    // 添加loadUserById方法以支持JwtAuthenticationFilter
    public CustomUserDetails loadUserById(Long userId) {
        // 依次检查不同类型的用户
        if (consumerService.findById(userId).isPresent()) {
            Consumer consumer = consumerService.findById(userId).get();
            ensurePasswordEncoded(consumer);
            return new CustomUserDetails(
                    consumer.getId(),
                    consumer.getUsername(),
                    consumer.getPassword(),
                    consumer.getEmail(),
                    consumer.getPhone(),
                    "consumer",
                    consumer.isEnabled(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + consumer.getRole().toUpperCase()))
            );
        } else if (supplierService.findById(userId).isPresent()) {
            Supplier supplier = supplierService.findById(userId).get();
            ensurePasswordEncoded(supplier);
            return new CustomUserDetails(
                    supplier.getId(),
                    supplier.getUsername(),
                    supplier.getPassword(),
                    supplier.getEmail(),
                    supplier.getPhone(),
                    "supplier",
                    supplier.isEnabled(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + supplier.getRole().toUpperCase()))
            );
        } else if (adminService.findById(userId).isPresent()) {
            Admin admin = adminService.findById(userId).get();
            ensurePasswordEncoded(admin);
            ensureAdminDefaults(admin);
            return new CustomUserDetails(
                    admin.getId(),
                    admin.getUsername(),
                    admin.getPassword(),
                    admin.getEmail(),
                    admin.getPhone(),
                    "admin",
                    admin.isEnabled(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + admin.getRole().toUpperCase()))
            );
        }
        throw new UsernameNotFoundException("用户不存在: " + userId);
    }

    /**
     * Automatically encode legacy plaintext passwords and normalize {bcrypt} prefixes to avoid login failures.
     */
    private void ensurePasswordEncoded(com.example.silkmall.entity.User user) {
        String existing = user.getPassword();
        if (existing == null || existing.isBlank()) {
            return;
        }
        String normalized = existing;
        if (normalized.startsWith("{bcrypt}")) {
            normalized = normalized.substring("{bcrypt}".length());
            user.setPassword(normalized);
            persistUser(user);
            return;
        }
        if (!PasswordHashValidator.isBcryptHash(normalized)) {
            user.setPassword(passwordEncoder.encode(normalized));
            persistUser(user);
        }
    }

    private void persistUser(com.example.silkmall.entity.User user) {
        if (user instanceof Consumer consumer) {
            consumerService.update(consumer);
        } else if (user instanceof Supplier supplier) {
            supplierService.update(supplier);
        } else if (user instanceof Admin admin) {
            adminService.update(admin);
        }
    }

    /**
     * Guardrail for legacy admin accounts missing role/permissions/enabled flags.
     */
    private void ensureAdminDefaults(Admin admin) {
        boolean dirty = false;
        if (admin.getRole() == null || admin.getRole().isBlank()) {
            admin.setRole("ADMIN");
            dirty = true;
        }
        if (admin.getPermissions() == null || admin.getPermissions().isBlank()) {
            admin.setPermissions("ALL");
            dirty = true;
        }
        if (!admin.isEnabled()) {
            admin.setEnabled(true);
            dirty = true;
        }
        if (dirty) {
            adminService.update(admin);
        }
    }
}
