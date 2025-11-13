package com.example.silkmall.controller;

import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController extends BaseController {
    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPLIER', 'CONSUMER')")
    public ResponseEntity<Map<String, BigDecimal>> balance(@AuthenticationPrincipal CustomUserDetails currentUser) {
        BigDecimal balance = walletService.getBalance(currentUser);
        return success(Map.of("balance", balance));
    }

    @PostMapping("/redeem")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPLIER', 'CONSUMER')")
    public ResponseEntity<Map<String, BigDecimal>> redeem(@RequestBody RedeemRequest request,
                                                           @AuthenticationPrincipal CustomUserDetails currentUser) {
        BigDecimal balance = walletService.redeem(currentUser, request.code());
        return success(Map.of("balance", balance));
    }

    public record RedeemRequest(String code) {}
}
