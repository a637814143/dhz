package com.example.silkmall.config;

import com.example.silkmall.entity.Admin;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.service.impl.NewAdminServiceImpl;
import com.example.silkmall.service.impl.NewConsumerServiceImpl;
import com.example.silkmall.service.impl.NewSupplierServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class DefaultAccountInitializer implements CommandLineRunner {

    private final NewConsumerServiceImpl consumerService;
    private final NewSupplierServiceImpl supplierService;
    private final NewAdminServiceImpl adminService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public DefaultAccountInitializer(NewConsumerServiceImpl consumerService,
                                     NewSupplierServiceImpl supplierService,
                                     NewAdminServiceImpl adminService,
                                     PasswordEncoder passwordEncoder) {
        this.consumerService = consumerService;
        this.supplierService = supplierService;
        this.adminService = adminService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        ensureConsumerAccount();
        ensureSupplierAccount();
        ensureAdminAccount();
    }

    private void ensureConsumerAccount() {
        final String username = "consumer_01";
        if (consumerService.existsByUsername(username)) {
            return;
        }

        Consumer consumer = new Consumer();
        consumer.setUsername(username);
        consumer.setPassword(passwordEncoder.encode("Consumer@123"));
        consumer.setEmail("consumer01@silkmall.cn");
        consumer.setPhone("13800000001");
        consumer.setAddress("江苏省苏州市工业园区蚕桑大道88号");
        consumer.setRole("consumer");
        consumer.setEnabled(true);
        consumer.setRealName("张小丝");
        consumer.setIdCard("320500199001011234");
        consumer.setPoints(1200);
        consumer.setMembershipLevel(2);
        consumerService.save(consumer);
    }

    private void ensureSupplierAccount() {
        final String username = "supplier_01";
        if (supplierService.existsByUsername(username)) {
            return;
        }

        Supplier supplier = new Supplier();
        supplier.setUsername(username);
        supplier.setPassword(passwordEncoder.encode("Supplier@123"));
        supplier.setEmail("supplier01@silkmall.cn");
        supplier.setPhone("13900000001");
        supplier.setAddress("浙江省杭州市蚕桑智谷66号");
        supplier.setRole("supplier");
        supplier.setEnabled(true);
        supplier.setCompanyName("丝路供应链科技有限公司");
        supplier.setBusinessLicense("91330100MA2HXXXXXX");
        supplier.setContactPerson("李云");
        supplier.setJoinDate(new Date());
        supplier.setSupplierLevel("A级合作伙伴");
        supplier.setStatus("active");
        supplierService.save(supplier);
    }

    private void ensureAdminAccount() {
        final String username = "admin_01";
        if (adminService.existsByUsername(username)) {
            return;
        }

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setEmail("admin01@silkmall.cn");
        admin.setPhone("13700000001");
        admin.setAddress("上海市浦东新区数字贸易中心28F");
        admin.setRole("admin");
        admin.setEnabled(true);
        admin.setDepartment("平台运营中心");
        admin.setPosition("系统管理员");
        admin.setPermissions("PRODUCT_MANAGE,ORDER_REVIEW,USER_AUDIT");
        adminService.save(admin);
    }
}
