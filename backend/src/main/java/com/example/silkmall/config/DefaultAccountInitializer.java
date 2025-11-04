package com.example.silkmall.config;

import com.example.silkmall.entity.Admin;
import com.example.silkmall.service.impl.NewAdminServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes the default administrator account. Historically the initializer attempted to
 * create the default administrator unconditionally which now fails because administrator
 * creation is disabled once at least one account exists. When administrators were removed
 * manually the application would fail to start because the initializer tried to create a
 * brand new account and {@link NewAdminServiceImpl#save(Admin)} rejected it with
 * "管理员账号不支持新建". This implementation only updates existing administrators and will
 * create a default one only when no administrators exist at all.
 */
@Component
public class DefaultAccountInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultAccountInitializer.class);

    private final NewAdminServiceImpl adminService;

    public DefaultAccountInitializer(NewAdminServiceImpl adminService) {
        this.adminService = adminService;
    }

    @Override
    public void run(String... args) {
        ensureAdminAccount("789", "admin789@example.com", "789");
    }

    private void ensureAdminAccount(String username, String email, String rawPassword) {
        adminService.findByUsername(username).ifPresentOrElse(existing -> {
            boolean updated = false;

            if (!"ADMIN".equals(existing.getRole())) {
                existing.setRole("ADMIN");
                updated = true;
            }

            if (existing.getPermissions() == null || existing.getPermissions().isBlank()) {
                existing.setPermissions("ALL");
                updated = true;
            }

            if (!existing.isEnabled()) {
                existing.setEnabled(true);
                updated = true;
            }

            if (updated) {
                adminService.save(existing);
                log.info("Updated default administrator account '{}'.", username);
            }
        }, () -> {
            if (adminService.findAll().isEmpty()) {
                Admin admin = new Admin();
                admin.setUsername(username);
                admin.setEmail(email);
                admin.setPassword(rawPassword);
                admin.setRole("ADMIN");
                admin.setPermissions("ALL");
                admin.setEnabled(true);
                adminService.register(admin);
                log.info("Created default administrator account '{}'.", username);
            } else {
                log.info("Skipped auto-creating administrator '{}' because at least one administrator already exists.", username);
            }
        });
    }
}
