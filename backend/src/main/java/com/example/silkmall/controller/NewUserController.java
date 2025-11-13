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
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class NewUserController extends BaseController {
    private final NewConsumerServiceImpl consumerService;
    private final NewSupplierServiceImpl supplierService;
    private final NewAdminServiceImpl adminService;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public NewUserController(NewConsumerServiceImpl consumerService, NewSupplierServiceImpl supplierService, 
                        NewAdminServiceImpl adminService, PasswordEncoder passwordEncoder) {
        this.consumerService = consumerService;
        this.supplierService = supplierService;
        this.adminService = adminService;
        this.passwordEncoder = passwordEncoder;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        // 依次检查不同类型的用户
        Optional<Consumer> consumer = consumerService.findById(id);
        if (consumer.isPresent()) {
            return success(consumer.get());
        }
        
        Optional<Supplier> supplier = supplierService.findById(id);
        if (supplier.isPresent()) {
            return success(supplier.get());
        }
        
        Optional<Admin> admin = adminService.findById(id);
        if (admin.isPresent()) {
            return success(admin.get());
        }
        
        return notFound("未找到该用户");
    }
    
    @PutMapping("/consumers/{id}")
    public ResponseEntity<?> updateConsumer(@PathVariable Long id, @RequestBody Consumer consumer) {
        Optional<Consumer> existingConsumer = consumerService.findById(id);
        if (!existingConsumer.isPresent()) {
            return notFound("未找到该消费者");
        }
        
        consumer.setId(id);
        // 如果密码不为空，则更新密码
        if (consumer.getPassword() != null && !consumer.getPassword().isEmpty()) {
            consumer.setPassword(passwordEncoder.encode(consumer.getPassword()));
        } else {
            // 否则保持原密码不变
            consumer.setPassword(existingConsumer.get().getPassword());
        }
        
        return success(consumerService.update(consumer));
    }
    
    @PutMapping("/suppliers/{id}")
    public ResponseEntity<?> updateSupplier(@PathVariable Long id, @RequestBody Supplier supplier) {
        Optional<Supplier> existingSupplier = supplierService.findById(id);
        if (!existingSupplier.isPresent()) {
            return notFound("未找到该供应商");
        }
        
        supplier.setId(id);
        // 如果密码不为空，则更新密码
        if (supplier.getPassword() != null && !supplier.getPassword().isEmpty()) {
            supplier.setPassword(passwordEncoder.encode(supplier.getPassword()));
        } else {
            // 否则保持原密码不变
            supplier.setPassword(existingSupplier.get().getPassword());
        }
        
        return success(supplierService.update(supplier));
    }
    
    @PutMapping("/admins/{id}")
    public ResponseEntity<?> updateAdmin(@PathVariable Long id, @RequestBody Admin admin) {
        Optional<Admin> existingAdmin = adminService.findById(id);
        if (!existingAdmin.isPresent()) {
            return notFound("未找到该管理员");
        }
        
        admin.setId(id);
        // 如果密码不为空，则更新密码
        if (admin.getPassword() != null && !admin.getPassword().isEmpty()) {
            admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        } else {
            // 否则保持原密码不变
            admin.setPassword(existingAdmin.get().getPassword());
        }
        
        return success(adminService.update(admin));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        // 依次检查并删除不同类型的用户
        if (consumerService.findById(id).isPresent()) {
            consumerService.deleteById(id);
            return success("消费者删除成功");
        }
        
        if (supplierService.findById(id).isPresent()) {
            supplierService.deleteById(id);
            return success("供应商删除成功");
        }
        
        if (adminService.findById(id).isPresent()) {
            return error("管理员账号不支持删除", HttpStatus.FORBIDDEN);
        }
        
        return notFound("未找到该用户");
    }
    
    @PostMapping("/consumers")
    public ResponseEntity<?> registerConsumer(@RequestBody Consumer consumer) {
        // 检查用户名是否已存在
        if (consumerService.existsByUsername(consumer.getUsername()) || 
            supplierService.existsByUsername(consumer.getUsername()) || 
            adminService.existsByUsername(consumer.getUsername())) {
            return badRequest("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (consumerService.existsByEmail(consumer.getEmail()) || 
            supplierService.existsByEmail(consumer.getEmail()) || 
            adminService.existsByEmail(consumer.getEmail())) {
            return badRequest("邮箱已存在");
        }
        
        // 加密密码
        consumer.setPassword(passwordEncoder.encode(consumer.getPassword()));
        consumer.setRole("consumer");
        
        return created(consumerService.register(consumer));
    }
    
    @PostMapping("/suppliers")
    public ResponseEntity<?> registerSupplier(@RequestBody Supplier supplier) {
        // 检查用户名是否已存在
        if (consumerService.existsByUsername(supplier.getUsername()) || 
            supplierService.existsByUsername(supplier.getUsername()) || 
            adminService.existsByUsername(supplier.getUsername())) {
            return badRequest("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (consumerService.existsByEmail(supplier.getEmail()) || 
            supplierService.existsByEmail(supplier.getEmail()) || 
            adminService.existsByEmail(supplier.getEmail())) {
            return badRequest("邮箱已存在");
        }
        
        // 加密密码
        supplier.setPassword(passwordEncoder.encode(supplier.getPassword()));
        supplier.setRole("supplier");
        
        return created(supplierService.register(supplier));
    }
    
    @PostMapping("/admins")
    public ResponseEntity<?> registerAdmin(@RequestBody Admin admin) {
        // 检查用户名是否已存在
        if (consumerService.existsByUsername(admin.getUsername()) || 
            supplierService.existsByUsername(admin.getUsername()) || 
            adminService.existsByUsername(admin.getUsername())) {
            return badRequest("用户名已存在");
        }
        
        // 检查邮箱是否已存在
        if (consumerService.existsByEmail(admin.getEmail()) || 
            supplierService.existsByEmail(admin.getEmail()) || 
            adminService.existsByEmail(admin.getEmail())) {
            return badRequest("邮箱已存在");
        }
        
        // 加密密码
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        admin.setRole("admin");
        
        return error("管理员账号仅支持登录，禁止注册", HttpStatus.FORBIDDEN);
    }
    
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<String> resetPassword(@PathVariable Long id, @RequestBody String newPassword) {
        // 依次检查并重置不同类型用户的密码
        if (consumerService.findById(id).isPresent()) {
            Consumer consumer = consumerService.findById(id).get();
            consumer.setPassword(passwordEncoder.encode(newPassword));
            consumerService.update(consumer);
            return success("密码重置成功");
        }
        
        if (supplierService.findById(id).isPresent()) {
            Supplier supplier = supplierService.findById(id).get();
            supplier.setPassword(passwordEncoder.encode(newPassword));
            supplierService.update(supplier);
            return success("密码重置成功");
        }
        
        if (adminService.findById(id).isPresent()) {
            Admin admin = adminService.findById(id).get();
            admin.setPassword(passwordEncoder.encode(newPassword));
            adminService.update(admin);
            return success("密码重置成功");
        }
        
        return notFound("未找到该用户");
    }
    
    @PostMapping("/{id}/enable")
    public ResponseEntity<String> enableUser(@PathVariable Long id) {
        // 依次检查并启用不同类型的用户
        if (consumerService.findById(id).isPresent()) {
            consumerService.enable(id);
            return success("用户已启用");
        }
        
        if (supplierService.findById(id).isPresent()) {
            supplierService.enable(id);
            return success("用户已启用");
        }
        
        if (adminService.findById(id).isPresent()) {
            adminService.enable(id);
            return success("用户已启用");
        }
        
        return notFound("未找到该用户");
    }
    
    @PostMapping("/{id}/disable")
    public ResponseEntity<String> disableUser(@PathVariable Long id) {
        // 依次检查并禁用不同类型的用户
        if (consumerService.findById(id).isPresent()) {
            consumerService.disable(id);
            return success("用户已禁用");
        }
        
        if (supplierService.findById(id).isPresent()) {
            supplierService.disable(id);
            return success("用户已禁用");
        }
        
        if (adminService.findById(id).isPresent()) {
            return error("管理员账号不支持禁用", HttpStatus.FORBIDDEN);
        }
        
        return notFound("未找到该用户");
    }
}