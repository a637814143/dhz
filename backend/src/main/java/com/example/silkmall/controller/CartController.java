package com.example.silkmall.controller;

import com.example.silkmall.dto.CartItemDTO;
import com.example.silkmall.entity.CartItem;
import com.example.silkmall.entity.Product;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
public class CartController extends BaseController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<?> list(@AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            return badRequest("请先登录消费者账号");
        }
        List<CartItemDTO> items = cartService.getCartItems(currentUser.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return success(items);
    }

    @PostMapping
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<?> addItem(@RequestBody CartItemRequest request,
                                     @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            return badRequest("请先登录消费者账号");
        }
        if (request.productId() == null) {
            return badRequest("请选择要加入购物车的商品");
        }
        int quantity = request.quantity() == null ? 1 : request.quantity();
        try {
            CartItem item = cartService.addItem(currentUser.getId(), request.productId(), quantity);
            return success(toDto(item));
        } catch (RuntimeException ex) {
            return badRequest(ex.getMessage());
        }
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<?> updateQuantity(@PathVariable Long itemId,
                                            @RequestBody CartItemRequest request,
                                            @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            return badRequest("请先登录消费者账号");
        }
        int quantity = request.quantity() == null ? 0 : request.quantity();
        try {
            CartItem item = cartService.updateQuantity(currentUser.getId(), itemId, quantity);
            if (item == null) {
                return success();
            }
            return success(toDto(item));
        } catch (RuntimeException ex) {
            return badRequest(ex.getMessage());
        }
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<?> removeItem(@PathVariable Long itemId,
                                        @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            return badRequest("请先登录消费者账号");
        }
        try {
            cartService.removeItem(currentUser.getId(), itemId);
            return success();
        } catch (RuntimeException ex) {
            return badRequest(ex.getMessage());
        }
    }

    private CartItemDTO toDto(CartItem item) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(item.getId());
        Integer quantity = item.getQuantity() == null ? 0 : item.getQuantity();
        dto.setQuantity(quantity);

        Product product = item.getProduct();
        BigDecimal unitPrice = product != null && product.getPrice() != null
                ? product.getPrice()
                : BigDecimal.ZERO;
        dto.setUnitPrice(unitPrice);
        dto.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        if (item.getCreatedAt() != null) {
            dto.setAddedAt(item.getCreatedAt().toInstant().toString());
        }

        CartItemDTO.ProductInfo productInfo = new CartItemDTO.ProductInfo();
        if (product != null) {
            productInfo.setId(product.getId());
            productInfo.setName(product.getName());
            productInfo.setMainImage(product.getMainImage());
            productInfo.setPrice(unitPrice);
            productInfo.setStatus(product.getStatus());
        }
        dto.setProduct(productInfo);
        return dto;
    }

    public record CartItemRequest(Long productId, Integer quantity) {
    }
}

