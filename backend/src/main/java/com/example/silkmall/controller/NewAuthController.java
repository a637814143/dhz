package com.example.silkmall.controller;
import com.example.silkmall.dto.LoginDTO;
import com.example.silkmall.dto.RegisterDTO;
import com.example.silkmall.dto.ResponseDTO;
import com.example.silkmall.entity.Admin;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.service.impl.NewAdminServiceImpl;
import com.example.silkmall.service.impl.NewConsumerServiceImpl;
import com.example.silkmall.service.impl.NewSupplierServiceImpl;
import com.example.silkmall.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class NewAuthController extends BaseController {
    private final NewConsumerServiceImpl consumerService;
    private final NewSupplierServiceImpl supplierService;
    private final NewAdminServiceImpl adminService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public NewAuthController(NewConsumerServiceImpl consumerService, NewSupplierServiceImpl supplierService, 
                        NewAdminServiceImpl adminService, PasswordEncoder passwordEncoder) {
        this.consumerService = consumerService;
        this.supplierService = supplierService;
        this.adminService = adminService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO loginDTO) {
        // 依次检查不同类型的用户
        Optional<Consumer> consumer = consumerService.findByUsername(loginDTO.getUsername());
        if (consumer.isPresent() && passwordEncoder.matches(loginDTO.getPassword(), consumer.get().getPassword())) {
            return success(consumer.get());
        }
        
        Optional<Supplier> supplier = supplierService.findByUsername(loginDTO.getUsername());
        if (supplier.isPresent() && passwordEncoder.matches(loginDTO.getPassword(), supplier.get().getPassword())) {
            return success(supplier.get());
        }
        
        Optional<Admin> admin = adminService.findByUsername(loginDTO.getUsername());
        if (admin.isPresent() && passwordEncoder.matches(loginDTO.getPassword(), admin.get().getPassword())) {
            return success(admin.get());
        }
        
        return badRequest("用户名或密码错误");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            return badRequest("两次输入的密码不一致");
        }
        
        // 依次检查不同类型用户的用户名和邮箱是否已存在
        if (consumerService.existsByUsername(registerDTO.getUsername()) || 
            supplierService.existsByUsername(registerDTO.getUsername()) || 
            adminService.existsByUsername(registerDTO.getUsername())) {
            return badRequest("用户名已存在");
        }
        
        if (consumerService.existsByEmail(registerDTO.getEmail()) || 
            supplierService.existsByEmail(registerDTO.getEmail()) || 
            adminService.existsByEmail(registerDTO.getEmail())) {
            return badRequest("邮箱已存在");
        }
        
        // 根据用户类型注册不同类型的用户
        switch (registerDTO.getUserType().toLowerCase()) {
            case "consumer":
                Consumer consumer = new Consumer();
                consumer.setUsername(registerDTO.getUsername());
                consumer.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
                consumer.setEmail(registerDTO.getEmail());
                consumer.setPhone(registerDTO.getPhone());
                consumer.setRole("consumer");
                return created(consumerService.register(consumer));
            case "supplier":
                Supplier supplier = new Supplier();
                supplier.setUsername(registerDTO.getUsername());
                supplier.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
                supplier.setEmail(registerDTO.getEmail());
                supplier.setPhone(registerDTO.getPhone());
                supplier.setRole("supplier");
                return created(supplierService.register(supplier));
            case "admin":
                Admin admin = new Admin();
                admin.setUsername(registerDTO.getUsername());
                admin.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
                admin.setEmail(registerDTO.getEmail());
                admin.setPhone(registerDTO.getPhone());
                admin.setRole("admin");
                return created(adminService.register(admin));
            default:
                return badRequest("不支持的角色类型");
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam String email) {
        // 依次检查不同类型的用户
        if (consumerService.findByEmail(email).isPresent() || 
            supplierService.findByEmail(email).isPresent() || 
            adminService.findByEmail(email).isPresent()) {
            // 这里应该是发送重置密码邮件的逻辑
            // 为了简化，我们直接返回成功消息
            return success("重置密码链接已发送到您的邮箱");
        } else {
            return notFound("未找到该邮箱对应的用户");
        }
    }
}