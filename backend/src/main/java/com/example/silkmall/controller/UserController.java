package com.example.silkmall.controller;

import com.example.silkmall.entity.Admin;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.entity.User;
import com.example.silkmall.service.AdminService;
import com.example.silkmall.service.ConsumerService;
import com.example.silkmall.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController extends BaseController {
    private final ConsumerService consumerService;
    private final SupplierService supplierService;
    private final AdminService adminService;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public UserController(ConsumerService consumerService, SupplierService supplierService, 
                        AdminService adminService, PasswordEncoder passwordEncoder) {
        this.consumerService = consumerService;
        this.supplierService = supplierService;
        this.adminService = adminService;
        this.passwordEncoder = passwordEncoder;
    }
    
    @GetMapping
    public ResponseEntity<List<?>> getAllUsers() {
        List<Object> allUsers = new ArrayList<>();
        allUsers.addAll(consumerService.findAll());
        allUsers.addAll(supplierService.findAll());
        allUsers.addAll(adminService.findAll());
        return success(allUsers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        // 依次检查不同类型的用户
        if (consumerService.findById(id).isPresent()) {
            return success(consumerService.findById(id).get());
        } else if (supplierService.findById(id).isPresent()) {
            return success(supplierService.findById(id).get());
        } else if (adminService.findById(id).isPresent()) {
            return success(adminService.findById(id).get());
        } else {
            return notFound("用户不存在");
        }
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        // 依次检查不同类型的用户
        if (consumerService.findByUsername(username).isPresent()) {
            return success(consumerService.findByUsername(username).get());
        } else if (supplierService.findByUsername(username).isPresent()) {
            return success(supplierService.findByUsername(username).get());
        } else if (adminService.findByUsername(username).isPresent()) {
            return success(adminService.findByUsername(username).get());
        } else {
            return notFound("用户不存在");
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<?> getUserByEmail(@PathVariable String email) {
        // 依次检查不同类型的用户
        if (consumerService.findByEmail(email).isPresent()) {
            return success(consumerService.findByEmail(email).get());
        } else if (supplierService.findByEmail(email).isPresent()) {
            return success(supplierService.findByEmail(email).get());
        } else if (adminService.findByEmail(email).isPresent()) {
            return success(adminService.findByEmail(email).get());
        } else {
            return notFound("用户不存在");
        }
    }
    
    @PutMapping("/consumers/{id}")
    public ResponseEntity<Consumer> updateConsumer(@PathVariable Long id, @RequestBody Consumer consumer) {
        consumer.setId(id);
        return success(consumerService.update(consumer));
    }
    
    @PutMapping("/suppliers/{id}")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        supplier.setId(id);
        return success(supplierService.update(supplier));
    }
    
    @PutMapping("/admins/{id}")
    public ResponseEntity<Admin> updateAdmin(@PathVariable Long id, @RequestBody Admin admin) {
        admin.setId(id);
        return success(adminService.update(admin));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // 依次尝试删除不同类型的用户
        if (consumerService.findById(id).isPresent()) {
            consumerService.deleteById(id);
        } else if (supplierService.findById(id).isPresent()) {
            supplierService.deleteById(id);
        } else if (adminService.findById(id).isPresent()) {
            adminService.deleteById(id);
        } else {
            throw new RuntimeException("用户不存在");
        }
        return success();
    }
    
    @PostMapping("/consumers/register")
    public ResponseEntity<Consumer> registerConsumer(@RequestBody Consumer consumer) {
        consumer.setRole("consumer");
        return created(consumerService.register(consumer));
    }
    
    @PostMapping("/suppliers/register")
    public ResponseEntity<Supplier> registerSupplier(@RequestBody Supplier supplier) {
        supplier.setRole("supplier");
        return created(supplierService.register(supplier));
    }
    
    @PostMapping("/admins/register")
    public ResponseEntity<Admin> registerAdmin(@RequestBody Admin admin) {
        admin.setRole("admin");
        return created(adminService.register(admin));
    }
    
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String email, @RequestParam String newPassword) {
        // 依次检查不同类型的用户
        if (consumerService.findByEmail(email).isPresent()) {
            Consumer consumer = consumerService.findByEmail(email).get();
            consumer.setPassword(passwordEncoder.encode(newPassword));
            consumerService.update(consumer);
        } else if (supplierService.findByEmail(email).isPresent()) {
            Supplier supplier = supplierService.findByEmail(email).get();
            supplier.setPassword(passwordEncoder.encode(newPassword));
            supplierService.update(supplier);
        } else if (adminService.findByEmail(email).isPresent()) {
            Admin admin = adminService.findByEmail(email).get();
            admin.setPassword(passwordEncoder.encode(newPassword));
            adminService.update(admin);
        } else {
            throw new RuntimeException("用户不存在: " + email);
        }
        
        return success("密码重置成功");
    }
    
    @PutMapping("/{id}/enable")
    public ResponseEntity<Void> enableUser(@PathVariable Long id) {
        // 依次尝试启用不同类型的用户
        if (consumerService.findById(id).isPresent()) {
            consumerService.enable(id);
        } else if (supplierService.findById(id).isPresent()) {
            supplierService.enable(id);
        } else if (adminService.findById(id).isPresent()) {
            adminService.enable(id);
        } else {
            throw new RuntimeException("用户不存在");
        }
        return success();
    }
    
    @PutMapping("/{id}/disable")
    public ResponseEntity<Void> disableUser(@PathVariable Long id) {
        // 依次尝试禁用不同类型的用户
        if (consumerService.findById(id).isPresent()) {
            consumerService.disable(id);
        } else if (supplierService.findById(id).isPresent()) {
            supplierService.disable(id);
        } else if (adminService.findById(id).isPresent()) {
            adminService.disable(id);
        } else {
            throw new RuntimeException("用户不存在");
        }
        return success();
    }
}