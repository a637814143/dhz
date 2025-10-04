package com.example.silkmall.controller;
import com.example.silkmall.dto.CaptchaChallengeDTO;
import com.example.silkmall.dto.LoginDTO;
import com.example.silkmall.dto.LoginResponseDTO;
import com.example.silkmall.dto.RegisterDTO;
import com.example.silkmall.dto.ResponseDTO;
import com.example.silkmall.dto.UserProfileDTO;
import com.example.silkmall.entity.Admin;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.security.CaptchaService;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.security.JwtTokenProvider;
import com.example.silkmall.service.impl.NewAdminServiceImpl;
import com.example.silkmall.service.impl.NewConsumerServiceImpl;
import com.example.silkmall.service.impl.NewSupplierServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class NewAuthController extends BaseController {
    private final NewConsumerServiceImpl consumerService;
    private final NewSupplierServiceImpl supplierService;
    private final NewAdminServiceImpl adminService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CaptchaService captchaService;

    @Autowired
    public NewAuthController(NewConsumerServiceImpl consumerService, NewSupplierServiceImpl supplierService,
                        NewAdminServiceImpl adminService,
                        AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider,
                        CaptchaService captchaService) {
        this.consumerService = consumerService;
        this.supplierService = supplierService;
        this.adminService = adminService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.captchaService = captchaService;
    }

    @GetMapping("/captcha")
    public ResponseEntity<ResponseDTO<CaptchaChallengeDTO>> createCaptcha() {
        CaptchaService.CaptchaChallenge challenge = captchaService.createChallenge();
        CaptchaChallengeDTO dto = new CaptchaChallengeDTO(
                challenge.id(),
                challenge.question(),
                challenge.expiresInSeconds()
        );
        return success(ResponseDTO.success(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO<LoginResponseDTO>> login(@Valid @RequestBody LoginDTO loginDTO) {
        boolean captchaValid = captchaService.validate(loginDTO.getChallengeId(), loginDTO.getVerificationCode());
        if (!captchaValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseDTO.error(HttpStatus.UNAUTHORIZED.value(), "验证码无效或已过期"));
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
            );
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseDTO.error(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误"));
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generateToken(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        UserProfileDTO profile = new UserProfileDTO(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                userDetails.getPhone(),
                userDetails.getUserType()
        );

        String redirectUrl = resolveRedirectUrl(userDetails.getUserType());
        long issuedAt = Instant.now().toEpochMilli();
        long expiresInSeconds = jwtTokenProvider.getJwtExpirationInSeconds();

        LoginResponseDTO payload = new LoginResponseDTO(token, expiresInSeconds, issuedAt, redirectUrl, profile);
        return success(ResponseDTO.success(payload));
    }

    @PostMapping("/register")
    public ResponseEntity<ResponseDTO<?>> register(@Valid @RequestBody RegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(ResponseDTO.error("两次输入的密码不一致"));
        }

        // 依次检查不同类型用户的用户名和邮箱是否已存在
        if (consumerService.existsByUsername(registerDTO.getUsername()) ||
            supplierService.existsByUsername(registerDTO.getUsername()) ||
            adminService.existsByUsername(registerDTO.getUsername())) {
            return ResponseEntity.badRequest().body(ResponseDTO.error("用户名已存在"));
        }

        if (consumerService.existsByEmail(registerDTO.getEmail()) ||
            supplierService.existsByEmail(registerDTO.getEmail()) ||
            adminService.existsByEmail(registerDTO.getEmail())) {
            return ResponseEntity.badRequest().body(ResponseDTO.error("邮箱已存在"));
        }

        // 根据用户类型注册不同类型的用户
        switch (registerDTO.getUserType().toLowerCase(Locale.ROOT)) {
            case "consumer":
                Consumer consumer = new Consumer();
                consumer.setUsername(registerDTO.getUsername());
                consumer.setPassword(registerDTO.getPassword());
                consumer.setEmail(registerDTO.getEmail());
                consumer.setPhone(registerDTO.getPhone());
                consumer.setRole("consumer");
                consumerService.register(consumer);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ResponseDTO.success());
            case "supplier":
                String companyName = registerDTO.getCompanyName() != null
                        ? registerDTO.getCompanyName().trim()
                        : "";
                if (companyName.isEmpty()) {
                    return ResponseEntity.badRequest().body(ResponseDTO.error("企业名称不能为空"));
                }
                Supplier supplier = new Supplier();
                supplier.setUsername(registerDTO.getUsername());
                supplier.setPassword(registerDTO.getPassword());
                supplier.setEmail(registerDTO.getEmail());
                supplier.setPhone(registerDTO.getPhone());
                supplier.setRole("supplier");
                supplier.setCompanyName(companyName);
                supplierService.register(supplier);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ResponseDTO.success());
            case "admin":
                Admin admin = new Admin();
                admin.setUsername(registerDTO.getUsername());
                admin.setPassword(registerDTO.getPassword());
                admin.setEmail(registerDTO.getEmail());
                admin.setPhone(registerDTO.getPhone());
                admin.setRole("admin");
                adminService.register(admin);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ResponseDTO.success());
            default:
                return ResponseEntity.badRequest().body(ResponseDTO.error("不支持的角色类型"));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ResponseDTO<String>> forgotPassword(@RequestParam String email) {
        // 依次检查不同类型的用户
        if (consumerService.findByEmail(email).isPresent() ||
            supplierService.findByEmail(email).isPresent() ||
            adminService.findByEmail(email).isPresent()) {
            // 这里应该是发送重置密码邮件的逻辑
            // 为了简化，我们直接返回成功消息
            return success(ResponseDTO.success("重置密码链接已发送到您的邮箱"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseDTO.error(HttpStatus.NOT_FOUND.value(), "未找到该邮箱对应的用户"));
        }
    }

    private String resolveRedirectUrl(String userType) {
        return switch (userType.toLowerCase(Locale.ROOT)) {
            case "admin" -> "/admin/overview";
            case "supplier" -> "/supplier/workbench";
            default -> "/consumer/dashboard";
        };
    }
}