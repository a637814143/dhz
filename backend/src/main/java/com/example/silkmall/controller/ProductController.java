package com.example.silkmall.controller;

import com.example.silkmall.dto.ProductOverviewDTO;
import com.example.silkmall.dto.ProductSummaryDTO;
import com.example.silkmall.entity.Product;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseController {
    private final ProductService productService;
    
    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        Optional<Product> product = productService.findById(id);
        if (product.isPresent()) {
            return success(product.get());
        } else {
            return notFound("产品不存在");
        }
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPLIER')")
    public ResponseEntity<?> createProduct(@RequestBody Product product,
                                           @AuthenticationPrincipal CustomUserDetails currentUser) {
        if (!canManageProduct(currentUser, product)) {
            return redirectForUser(currentUser);
        }
        return created(productService.save(product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPLIER')")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product product,
                                           @AuthenticationPrincipal CustomUserDetails currentUser) {
        Optional<Product> existing = productService.findById(id);
        if (existing.isEmpty()) {
            return notFound("产品不存在");
        }
        if (!canManageProduct(currentUser, existing.get())) {
            return redirectForUser(currentUser);
        }
        Product toUpdate = existing.get();

        // 始终保持创建时间不变，避免更新时写入 null 值
        if (product.getName() != null) {
            toUpdate.setName(product.getName());
        }
        toUpdate.setDescription(product.getDescription());
        if (product.getPrice() != null) {
            toUpdate.setPrice(product.getPrice());
        }
        if (product.getStock() != null) {
            toUpdate.setStock(product.getStock());
        }
        if (product.getSales() != null) {
            toUpdate.setSales(product.getSales());
        }
        if (product.getStatus() != null) {
            toUpdate.setStatus(product.getStatus());
        }
        toUpdate.setMainImage(product.getMainImage());
        toUpdate.setCategory(product.getCategory());
        if (product.getSupplier() != null) {
            toUpdate.setSupplier(product.getSupplier());
        }

        return success(productService.save(toUpdate));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPLIER')")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id,
                                           @AuthenticationPrincipal CustomUserDetails currentUser) {
        Optional<Product> existing = productService.findById(id);
        if (existing.isEmpty()) {
            return notFound("产品不存在");
        }
        if (!canManageProduct(currentUser, existing.get())) {
            return redirectForUser(currentUser);
        }
        productService.deleteById(id);
        return success();
    }
    
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return success(productService.findAll());
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<Product>> getProductsByStatus(@PathVariable String status, Pageable pageable) {
        return success(productService.findByStatus(status, pageable));
    }
    
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<Product>> getProductsByCategoryId(@PathVariable Long categoryId, Pageable pageable) {
        return success(productService.findByCategoryId(categoryId, pageable));
    }
    
    @GetMapping("/supplier/{supplierId}")
    public ResponseEntity<Page<Product>> getProductsBySupplierId(@PathVariable Long supplierId, Pageable pageable) {
        return success(productService.findBySupplierId(supplierId, pageable));
    }
    
    @GetMapping("/top-sales")
    public ResponseEntity<List<Product>> getTopSalesProducts() {
        return success(productService.findTop10ByOrderBySalesDesc());
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<Product>> searchProducts(@RequestParam String keyword, Pageable pageable) {
        return success(productService.search(keyword, pageable));
    }

    @GetMapping("/advanced-search")
    public ResponseEntity<Page<ProductSummaryDTO>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction sortDirection) {
        Set<String> allowedSortFields = Set.of("createdAt", "price", "sales", "stock", "name");
        if (!allowedSortFields.contains(sortBy)) {
            sortBy = "createdAt";
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(sortDirection, sortBy));
        Page<Product> products = productService.advancedSearch(
                keyword,
                categoryId,
                supplierId,
                minPrice,
                maxPrice,
                status,
                pageable);

        Page<ProductSummaryDTO> dtoPage = products.map(this::toSummaryDTO);
        return success(new PageImpl<>(dtoPage.getContent(), pageable, products.getTotalElements()));
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPLIER')")
    public ResponseEntity<?> updateStock(@PathVariable Long id, @RequestParam Integer stock,
                                         @AuthenticationPrincipal CustomUserDetails currentUser) {
        Optional<Product> existing = productService.findById(id);
        if (existing.isEmpty()) {
            return notFound("产品不存在");
        }
        if (!canManageProduct(currentUser, existing.get())) {
            return redirectForUser(currentUser);
        }
        productService.updateStock(id, stock);
        return success();
    }

    @PutMapping("/{id}/on-sale")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPLIER')")
    public ResponseEntity<?> putProductOnSale(@PathVariable Long id,
                                              @AuthenticationPrincipal CustomUserDetails currentUser) {
        Optional<Product> existing = productService.findById(id);
        if (existing.isEmpty()) {
            return notFound("产品不存在");
        }
        if (!canManageProduct(currentUser, existing.get())) {
            return redirectForUser(currentUser);
        }
        productService.putProductOnSale(id);
        return success();
    }

    @PutMapping("/{id}/off-sale")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPLIER')")
    public ResponseEntity<?> takeProductOffSale(@PathVariable Long id,
                                                @AuthenticationPrincipal CustomUserDetails currentUser) {
        Optional<Product> existing = productService.findById(id);
        if (existing.isEmpty()) {
            return notFound("产品不存在");
        }
        if (!canManageProduct(currentUser, existing.get())) {
            return redirectForUser(currentUser);
        }
        productService.takeProductOffSale(id);
        return success();
    }

    @GetMapping("/overview")
    public ResponseEntity<ProductOverviewDTO> getProductOverview() {
        return success(productService.getProductOverview());
    }

    private ProductSummaryDTO toSummaryDTO(Product product) {
        ProductSummaryDTO dto = new ProductSummaryDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
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

    private boolean canManageProduct(CustomUserDetails user, Product product) {
        if (user == null) {
            return false;
        }
        if ("admin".equalsIgnoreCase(user.getUserType())) {
            return true;
        }
        if (!"supplier".equalsIgnoreCase(user.getUserType())) {
            return false;
        }
        if (product == null || product.getSupplier() == null || product.getSupplier().getId() == null) {
            return false;
        }
        return product.getSupplier().getId().equals(user.getId());
    }

    private ResponseEntity<?> redirectForUser(CustomUserDetails currentUser) {
        String target = "/login";
        if (currentUser != null) {
            switch (currentUser.getUserType().toLowerCase()) {
                case "admin" -> target = "/admin/overview";
                case "supplier" -> target = "/supplier/workbench";
                case "consumer" -> target = "/consumer/dashboard";
                default -> target = "/login";
            }
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, target);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}