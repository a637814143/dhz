package com.example.silkmall.controller;

import com.example.silkmall.dto.ConsumerFavoriteDTO;
import com.example.silkmall.dto.ProductSummaryDTO;
import com.example.silkmall.entity.ConsumerFavorite;
import com.example.silkmall.entity.Product;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.service.ConsumerFavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
public class ConsumerFavoriteController extends BaseController {

    private final ConsumerFavoriteService favoriteService;

    public ConsumerFavoriteController(ConsumerFavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<?> list(@AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            return badRequest("请先登录消费者账号");
        }
        List<ConsumerFavoriteDTO> favorites = favoriteService.listFavorites(currentUser.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return success(favorites);
    }

    @PostMapping
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<?> add(@RequestBody FavoriteRequest request,
                                 @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            return badRequest("请先登录消费者账号");
        }
        if (request.productId() == null) {
            return badRequest("请选择要收藏的商品");
        }
        try {
            ConsumerFavorite favorite = favoriteService.addFavorite(currentUser.getId(), request.productId());
            return success(toDto(favorite));
        } catch (RuntimeException ex) {
            return badRequest(ex.getMessage());
        }
    }

    @DeleteMapping("/{favoriteId}")
    @PreAuthorize("hasRole('CONSUMER')")
    public ResponseEntity<?> remove(@PathVariable Long favoriteId,
                                    @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (currentUser == null) {
            return badRequest("请先登录消费者账号");
        }
        try {
            favoriteService.removeFavorite(currentUser.getId(), favoriteId);
            return success();
        } catch (RuntimeException ex) {
            return badRequest(ex.getMessage());
        }
    }

    private ConsumerFavoriteDTO toDto(ConsumerFavorite favorite) {
        ConsumerFavoriteDTO dto = new ConsumerFavoriteDTO();
        dto.setId(favorite.getId());
        if (favorite.getCreatedAt() != null) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            dto.setCreatedAt(formatter.format(favorite.getCreatedAt()));
        }
        dto.setProduct(toSummary(favorite.getProduct()));
        return dto;
    }

    private ProductSummaryDTO toSummary(Product product) {
        if (product == null) {
            return null;
        }
        ProductSummaryDTO dto = new ProductSummaryDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setUnit(product.getUnit());
        dto.setStock(product.getStock());
        dto.setSales(product.getSales());
        dto.setMainImage(product.getMainImage());
        dto.setStatus(product.getStatus());
        dto.setCreatedAt(product.getCreatedAt());
        if (product.getCategory() != null) {
            dto.setCategoryName(product.getCategory().getName());
        }
        if (product.getSupplier() != null) {
            dto.setSupplierName(product.getSupplier().getCompanyName());
            dto.setSupplierLevel(product.getSupplier().getSupplierLevel());
        }
        return dto;
    }

    public record FavoriteRequest(Long productId) {
    }
}
