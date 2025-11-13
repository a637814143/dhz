package com.example.silkmall.service;

import com.example.silkmall.entity.Admin;
import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.entity.User;
import com.example.silkmall.repository.AdminRepository;
import com.example.silkmall.repository.ConsumerRepository;
import com.example.silkmall.repository.SupplierRepository;
import com.example.silkmall.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class WalletService {
    private static final BigDecimal REDEEM_AMOUNT = BigDecimal.valueOf(1000L);
    private static final String DEFAULT_REDEEMABLE_HASH = "b9ae921e3a739d600f969c62344b20ab";

    private final ConsumerRepository consumerRepository;
    private final SupplierRepository supplierRepository;
    private final AdminRepository adminRepository;
    private final Set<String> redeemableHashes;

    public WalletService(ConsumerRepository consumerRepository,
                         SupplierRepository supplierRepository,
                         AdminRepository adminRepository,
                         @Value("${wallet.redeemable-md5-codes:b9ae921e3a739d600f969c62344b20ab}") String hashedCodes) {
        this.consumerRepository = consumerRepository;
        this.supplierRepository = supplierRepository;
        this.adminRepository = adminRepository;
        this.redeemableHashes = new CopyOnWriteArraySet<>(parseRedeemableHashes(hashedCodes));
        if (this.redeemableHashes.isEmpty()) {
            this.redeemableHashes.add(DEFAULT_REDEEMABLE_HASH);
        }
    }

    public BigDecimal getBalance(CustomUserDetails currentUser) {
        User user = requireUser(currentUser);
        return Optional.ofNullable(user.getWalletBalance()).orElse(BigDecimal.valueOf(1000L));
    }

    public BigDecimal redeem(CustomUserDetails currentUser, String code) {
        if (code == null || code.isBlank()) {
            throw new RuntimeException("兑换码不能为空");
        }
        String hash = DigestUtils.md5DigestAsHex(code.trim().getBytes(StandardCharsets.UTF_8));
        if (!redeemableHashes.remove(hash)) {
            throw new RuntimeException("兑换码无效或已被使用");
        }

        User user = requireUser(currentUser);
        BigDecimal balance = Optional.ofNullable(user.getWalletBalance()).orElse(BigDecimal.valueOf(1000L));
        BigDecimal updated = balance.add(REDEEM_AMOUNT);
        user.setWalletBalance(updated);
        saveUser(user);
        return updated;
    }

    public void adjustBalance(Long userId, String role, BigDecimal delta) {
        if (userId == null || delta == null) {
            return;
        }
        User user = findUserByRole(userId, role)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        BigDecimal balance = Optional.ofNullable(user.getWalletBalance()).orElse(BigDecimal.valueOf(1000L));
        user.setWalletBalance(balance.add(delta));
        if (user.getWalletBalance().compareTo(BigDecimal.ZERO) < 0) {
            user.setWalletBalance(BigDecimal.ZERO);
        }
        saveUser(user);
    }

    private List<String> parseRedeemableHashes(String hashedCodes) {
        if (hashedCodes == null || hashedCodes.isBlank()) {
            return List.of(DEFAULT_REDEEMABLE_HASH);
        }
        return Arrays.stream(hashedCodes.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(code -> code.toLowerCase(Locale.ROOT))
                .toList();
    }

    private User requireUser(CustomUserDetails currentUser) {
        if (currentUser == null) {
            throw new RuntimeException("未登录或登录已过期");
        }
        return findUserByRole(currentUser.getId(), currentUser.getUserType())
                .orElseThrow(() -> new RuntimeException("无法找到当前用户信息"));
    }

    private Optional<User> findUserByRole(Long id, String role) {
        if (id == null || role == null) {
            return Optional.empty();
        }
        return switch (role.toLowerCase()) {
            case "consumer" -> consumerRepository.findById(id).map(user -> (User) user);
            case "supplier" -> supplierRepository.findById(id).map(user -> (User) user);
            case "admin" -> adminRepository.findById(id).map(user -> (User) user);
            default -> Optional.empty();
        };
    }

    private void saveUser(User user) {
        if (user instanceof Consumer consumer) {
            consumerRepository.save(consumer);
        } else if (user instanceof Supplier supplier) {
            supplierRepository.save(supplier);
        } else if (user instanceof Admin admin) {
            adminRepository.save(admin);
        } else {
            throw new RuntimeException("不支持的钱包用户类型");
        }
    }
}
