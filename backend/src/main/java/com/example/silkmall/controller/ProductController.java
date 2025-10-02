package com.example.silkmall.controller;

import com.example.silkmall.dto.ProductOverviewDTO;
import com.example.silkmall.dto.ProductSummaryDTO;
import com.example.silkmall.entity.Product;
import com.example.silkmall.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        return created(productService.save(product));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        return success(productService.save(product));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
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
    public ResponseEntity<Void> updateStock(@PathVariable Long id, @RequestParam Integer stock) {
        productService.updateStock(id, stock);
        return success();
    }
    
    @PutMapping("/{id}/on-sale")
    public ResponseEntity<Void> putProductOnSale(@PathVariable Long id) {
        productService.putProductOnSale(id);
        return success();
    }
    
    @PutMapping("/{id}/off-sale")
    public ResponseEntity<Void> takeProductOffSale(@PathVariable Long id) {
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
}