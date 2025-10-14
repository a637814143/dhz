package com.example.silkmall.controller;

import com.example.silkmall.dto.ProductDetailDTO;
import com.example.silkmall.dto.ProductOverviewDTO;
import com.example.silkmall.dto.ProductSummaryDTO;
import com.example.silkmall.entity.Category;
import com.example.silkmall.entity.Product;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.service.ProductService;
import com.example.silkmall.service.SupplierService;
import com.example.silkmall.service.CategoryService;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController extends BaseController {
    private final ProductService productService;
    private final SupplierService supplierService;
    private final CategoryService categoryService;

    @Autowired
    public ProductController(ProductService productService,
                             SupplierService supplierService,
                             CategoryService categoryService) {
        this.productService = productService;
        this.supplierService = supplierService;
        this.categoryService = categoryService;
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        Optional<Product> product = productService.findById(id);
        if (product.isPresent()) {
            return success(toDetailDTO(product.get()));
        }
        return notFound("产品不存在");
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPPLIER')")
    public ResponseEntity<?> createProduct(@RequestBody Product product,
                                           @AuthenticationPrincipal CustomUserDetails currentUser) {
        product.setId(null);

        ResponseEntity<?> supplierResponse = attachSupplierForCreation(product, currentUser);
        if (supplierResponse != null) {
            return supplierResponse;
        }

        ResponseEntity<?> categoryResponse = applyCategoryFromPayload(product, product);
        if (categoryResponse != null) {
            return categoryResponse;
        }

        product.setName(normalize(product.getName()));
        product.setDescription(normalize(product.getDescription()));
        product.setMainImage(normalize(product.getMainImage()));
        product.setStatus(normalizeStatus(product.getStatus(), "OFF_SALE"));

        ResponseEntity<?> validationError = validateProductBasics(product);
        if (validationError != null) {
            return validationError;
        }

        Product saved = productService.save(product);
        return created(toSummaryDTO(saved));
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

        toUpdate.setName(normalize(product.getName()));
        toUpdate.setDescription(normalize(product.getDescription()));
        toUpdate.setPrice(product.getPrice());
        toUpdate.setStock(product.getStock());
        toUpdate.setStatus(normalizeStatus(product.getStatus(), toUpdate.getStatus()));
        toUpdate.setMainImage(normalize(product.getMainImage()));

        ResponseEntity<?> categoryResponse = applyCategoryFromPayload(toUpdate, product);
        if (categoryResponse != null) {
            return categoryResponse;
        }

        ResponseEntity<?> validationError = validateProductBasics(toUpdate);
        if (validationError != null) {
            return validationError;
        }

        Product saved = productService.save(toUpdate);
        return success(toSummaryDTO(saved));
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
    public ResponseEntity<Page<ProductSummaryDTO>> getProductsBySupplierId(@PathVariable Long supplierId, Pageable pageable) {
        Page<Product> products = productService.findBySupplierId(supplierId, pageable);
        Page<ProductSummaryDTO> dtoPage = products.map(this::toSummaryDTO);
        return success(dtoPage);
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
        dto.setStock(product.getStock() != null ? product.getStock() : 0);
        dto.setSales(product.getSales() != null ? product.getSales() : 0);
        dto.setMainImage(product.getMainImage());
        dto.setStatus(product.getStatus());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        if (product.getSupplier() != null) {
            dto.setSupplierId(product.getSupplier().getId());
            dto.setSupplierName(product.getSupplier().getCompanyName());
            dto.setSupplierLevel(product.getSupplier().getSupplierLevel());
        }
        return dto;
    }

    private ProductDetailDTO toDetailDTO(Product product) {
        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStock(product.getStock() != null ? product.getStock() : 0);
        dto.setSales(product.getSales() != null ? product.getSales() : 0);
        dto.setMainImage(product.getMainImage());
        dto.setStatus(product.getStatus());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());

        if (product.getCategory() != null) {
            ProductDetailDTO.CategoryInfo categoryInfo = new ProductDetailDTO.CategoryInfo();
            categoryInfo.setId(product.getCategory().getId());
            categoryInfo.setName(product.getCategory().getName());
            categoryInfo.setDescription(product.getCategory().getDescription());
            dto.setCategory(categoryInfo);
        } else {
            dto.setCategory(null);
        }

        if (product.getSupplier() != null) {
            ProductDetailDTO.SupplierInfo supplierInfo = new ProductDetailDTO.SupplierInfo();
            supplierInfo.setId(product.getSupplier().getId());
            supplierInfo.setCompanyName(product.getSupplier().getCompanyName());
            supplierInfo.setSupplierLevel(product.getSupplier().getSupplierLevel());
            supplierInfo.setContactName(product.getSupplier().getContactPerson());
            supplierInfo.setContactPhone(product.getSupplier().getPhone());
            dto.setSupplier(supplierInfo);
        } else {
            dto.setSupplier(null);
        }

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            dto.setImages(product.getImages().stream().map(image -> {
                ProductDetailDTO.ImageInfo imageInfo = new ProductDetailDTO.ImageInfo();
                imageInfo.setId(image.getId());
                imageInfo.setImageUrl(image.getImageUrl());
                imageInfo.setSortOrder(image.getSortOrder());
                imageInfo.setCreatedAt(image.getCreatedAt());
                return imageInfo;
            }).collect(Collectors.toList()));
        } else {
            dto.setImages(Collections.emptyList());
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

    private ResponseEntity<?> attachSupplierForCreation(Product product, CustomUserDetails currentUser) {
        if (currentUser == null) {
            return redirectForUser(null);
        }

        Supplier supplier;
        if ("admin".equalsIgnoreCase(currentUser.getUserType())) {
            Supplier requested = product.getSupplier();
            Long supplierId = requested != null ? requested.getId() : null;
            if (supplierId == null) {
                return badRequest("请为商品指定供应商");
            }
            supplier = supplierService.findById(supplierId)
                    .orElse(null);
            if (supplier == null) {
                return notFound("供应商不存在");
            }
        } else if ("supplier".equalsIgnoreCase(currentUser.getUserType())) {
            supplier = supplierService.findById(currentUser.getId())
                    .orElse(null);
            if (supplier == null) {
                return notFound("供应商不存在");
            }
        } else {
            return redirectForUser(currentUser);
        }

        product.setSupplier(supplier);
        return null;
    }

    private ResponseEntity<?> applyCategoryFromPayload(Product target, Product source) {
        if (source == null || source.getCategory() == null) {
            target.setCategory(null);
            return null;
        }

        Category requestedCategory = source.getCategory();
        Long categoryId = requestedCategory.getId();
        if (categoryId == null) {
            target.setCategory(null);
            return null;
        }

        Category category = categoryService.findById(categoryId)
                .orElse(null);
        if (category == null) {
            return notFound("分类不存在");
        }

        target.setCategory(category);
        return null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatus(String requested, String fallback) {
        String normalized = normalize(requested);
        if (normalized == null) {
            return fallback;
        }
        return normalized.toUpperCase();
    }

    private ResponseEntity<?> validateProductBasics(Product product) {
        if (product.getName() == null) {
            return badRequest("商品名称不能为空");
        }
        if (product.getPrice() == null) {
            return badRequest("商品价格不能为空");
        }
        if (product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            return badRequest("商品价格不能为负数");
        }
        if (product.getStock() != null && product.getStock() < 0) {
            return badRequest("库存数量不能为负数");
        }
        return null;
    }
}
