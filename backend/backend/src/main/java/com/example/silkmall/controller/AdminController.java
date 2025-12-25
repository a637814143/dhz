package com.example.silkmall.controller;

import com.example.silkmall.dto.AdminProfileDTO;
import com.example.silkmall.entity.Admin;
import com.example.silkmall.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
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
            return success(toProfile(admin.get()));
        } else {
            return notFound("管理员不存在");
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminProfileDTO> updateAdmin(@PathVariable Long id, @RequestBody AdminProfileDTO payload) {
        Optional<Admin> existing = adminService.findById(id);
        if (existing.isEmpty()) {
            return notFound("管理员不存在");
        }

        Admin admin = existing.get();
        admin.setUsername(payload.getUsername());
        admin.setEmail(payload.getEmail());
        admin.setPhone(payload.getPhone());
        admin.setAddress(payload.getAddress());
        admin.setDepartment(payload.getDepartment());
        admin.setPosition(payload.getPosition());

        Admin saved = adminService.update(admin);
        return success(toProfile(saved));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAdmin(@PathVariable Long id) {
        adminService.deleteById(id);
        return success();
    }
    
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Admin> register(@RequestBody Admin admin) {
        return created(adminService.register(admin));
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

    private AdminProfileDTO toProfile(Admin admin) {
        AdminProfileDTO dto = new AdminProfileDTO();
        dto.setId(admin.getId());
        dto.setUsername(admin.getUsername());
        dto.setEmail(admin.getEmail());
        dto.setPhone(admin.getPhone());
        dto.setAddress(admin.getAddress());
        dto.setDepartment(admin.getDepartment());
        dto.setPosition(admin.getPosition());
        return dto;
    }
}
