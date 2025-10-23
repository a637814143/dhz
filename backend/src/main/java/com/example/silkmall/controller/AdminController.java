package com.example.silkmall.controller;

import com.example.silkmall.entity.Admin;
import com.example.silkmall.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/admins")
public class AdminController extends BaseController {
    private final AdminService adminService;
    
    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }
    
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAdminById(@PathVariable Long id) {
        Optional<Admin> admin = adminService.findById(id);
        if (admin.isPresent()) {
            return success(admin.get());
        } else {
            return notFound("管理员不存在");
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateAdmin(@PathVariable Long id, @RequestBody Admin admin) {
        Optional<Admin> existing = adminService.findById(id);
        if (existing.isEmpty()) {
            return notFound("管理员不存在");
        }

        admin.setId(id);
        if (admin.getPassword() == null || admin.getPassword().isEmpty()) {
            admin.setPassword(existing.get().getPassword());
        }

        return success(adminService.update(admin));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteAdmin(@PathVariable Long id) {
        if (adminService.findById(id).isEmpty()) {
            return notFound("管理员不存在");
        }
        return error("管理员账号不支持删除", HttpStatus.FORBIDDEN);
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> register(@RequestBody Admin admin) {
        return error("管理员账号仅支持登录，禁止注册", HttpStatus.FORBIDDEN);
    }
    
    @GetMapping("/{id}/permissions/{permission}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> hasPermission(@PathVariable Long id, @PathVariable String permission) {
        return success(adminService.hasPermission(id, permission));
    }
    
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updatePermissions(@PathVariable Long id, @RequestBody String permissions) {
        adminService.updatePermissions(id, permissions);
        return success();
    }
}