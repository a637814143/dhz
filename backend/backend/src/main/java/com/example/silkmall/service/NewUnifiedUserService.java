import com.example.silkmall.entity.Admin;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.service.impl.NewAdminServiceImpl;
import com.example.silkmall.service.impl.NewConsumerServiceImpl;
import com.example.silkmall.service.impl.NewSupplierServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class NewUnifiedUserService {
    private final NewConsumerServiceImpl consumerService;
    private final NewSupplierServiceImpl supplierService;
    private final NewAdminServiceImpl adminService;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public NewUnifiedUserService(NewConsumerServiceImpl consumerService, 
                               NewSupplierServiceImpl supplierService, 
                               NewAdminServiceImpl adminService, 
                               PasswordEncoder passwordEncoder) {
        this.consumerService = consumerService;
        this.supplierService = supplierService;
        this.adminService = adminService;
        this.passwordEncoder = passwordEncoder;
    }
    
    // 查找用户（任何类型）
    public Optional<Object> findByUsername(String username) {
        if (consumerService.findByUsername(username).isPresent()) {
            return Optional.of(consumerService.findByUsername(username).get());
        } else if (supplierService.findByUsername(username).isPresent()) {
            return Optional.of(supplierService.findByUsername(username).get());
        } else if (adminService.findByUsername(username).isPresent()) {
            return Optional.of(adminService.findByUsername(username).get());
        }
        return Optional.empty();
    }
    
    // 检查用户名是否存在（任何类型）
    public boolean existsByUsername(String username) {
        return consumerService.existsByUsername(username) || 
               supplierService.existsByUsername(username) || 
               adminService.existsByUsername(username);
    }
    
    // 检查邮箱是否存在（任何类型）
    public boolean existsByEmail(String email) {
        return consumerService.existsByEmail(email) || 
               supplierService.existsByEmail(email) || 
               adminService.existsByEmail(email);
    }
    
    // 通过邮箱查找用户（任何类型）
    public Optional<Object> findByEmail(String email) {
        if (consumerService.findByEmail(email).isPresent()) {
            return Optional.of(consumerService.findByEmail(email).get());
        } else if (supplierService.findByEmail(email).isPresent()) {
            return Optional.of(supplierService.findByEmail(email).get());
        } else if (adminService.findByEmail(email).isPresent()) {
            return Optional.of(adminService.findByEmail(email).get());
        }
        return Optional.empty();
    }
    
    // 消费者相关操作
    public Consumer registerConsumer(Consumer consumer) {
        consumer.setRole("consumer");
        return consumerService.register(consumer);
    }
    
    public Optional<Consumer> findConsumerById(Long id) {
        return consumerService.findById(id);
    }
    
    public Consumer updateConsumer(Consumer consumer) {
        return consumerService.update(consumer);
    }
    
    // 供应商相关操作
    public Supplier registerSupplier(Supplier supplier) {
        supplier.setRole("supplier");
        return supplierService.register(supplier);
    }
    
    public Optional<Supplier> findSupplierById(Long id) {
        return supplierService.findById(id);
    }
    
    public Supplier updateSupplier(Supplier supplier) {
        return supplierService.update(supplier);
    }
    
    // 管理员相关操作
    public Admin registerAdmin(Admin admin) {
        admin.setRole("admin");
        return adminService.register(admin);
    }
    
    public Optional<Admin> findAdminById(Long id) {
        return adminService.findById(id);
    }
    
    public Admin updateAdmin(Admin admin) {
        return adminService.update(admin);
    }
    
    // 通用用户操作
    public Optional<Object> findUserById(Long id) {
        if (consumerService.findById(id).isPresent()) {
            return Optional.of(consumerService.findById(id).get());
        } else if (supplierService.findById(id).isPresent()) {
            return Optional.of(supplierService.findById(id).get());
        } else if (adminService.findById(id).isPresent()) {
            return Optional.of(adminService.findById(id).get());
        }
        return Optional.empty();
    }
    
    public void deleteUserById(Long id) {
        if (consumerService.findById(id).isPresent()) {
            consumerService.deleteById(id);
        } else if (supplierService.findById(id).isPresent()) {
            supplierService.deleteById(id);
        } else if (adminService.findById(id).isPresent()) {
            adminService.deleteById(id);
        } else {
            throw new RuntimeException("用户不存在");
        }
    }
    
    public void enableUser(Long id) {
        if (consumerService.findById(id).isPresent()) {
            consumerService.enable(id);
        } else if (supplierService.findById(id).isPresent()) {
            supplierService.enable(id);
        } else if (adminService.findById(id).isPresent()) {
            adminService.enable(id);
        } else {
            throw new RuntimeException("用户不存在");
        }
    }
    
    public void disableUser(Long id) {
        if (consumerService.findById(id).isPresent()) {
            consumerService.disable(id);
        } else if (supplierService.findById(id).isPresent()) {
            supplierService.disable(id);
        } else if (adminService.findById(id).isPresent()) {
            adminService.disable(id);
        } else {
            throw new RuntimeException("用户不存在");
        }
    }
    
    // 重置密码
    public void resetPassword(String email, String newPassword) {
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
    }
}