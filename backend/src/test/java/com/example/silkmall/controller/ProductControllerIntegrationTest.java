package com.example.silkmall.controller;

import com.example.silkmall.dto.ProductDetailDTO;
import com.example.silkmall.dto.ProductSummaryDTO;
import com.example.silkmall.entity.Category;
import com.example.silkmall.entity.Product;
import com.example.silkmall.entity.Supplier;
import com.example.silkmall.security.CustomUserDetails;
import com.example.silkmall.service.CategoryService;
import com.example.silkmall.service.ProductService;
import com.example.silkmall.service.SupplierService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-controller-test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.main.allow-circular-references=true"
})
class ProductControllerIntegrationTest {

    @Autowired
    private ProductController productController;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Test
    void supplierCreatedProductIsPersistedWithAssociations() {
        Supplier supplier = new Supplier();
        supplier.setUsername("supplier_integration");
        supplier.setPassword("password123");
        supplier.setEmail("supplier.integration@example.com");
        supplier.setPhone("13800138000");
        supplier.setRole("SUPPLIER");
        supplier.setEnabled(true);
        supplier.setCompanyName("集成测试供应商");
        supplier.setContactPerson("测试联系人");
        supplier.setSupplierLevel("GOLD");
        supplier = supplierService.save(supplier);

        Category category = new Category();
        category.setName("测试分类");
        category.setDescription("用于集成测试的分类");
        category.setEnabled(true);
        category = categoryService.save(category);

        Product requestPayload = new Product();
        requestPayload.setName("集成测试商品");
        requestPayload.setDescription("确保商品能够正确保存和查询");
        requestPayload.setPrice(new BigDecimal("199.90"));
        requestPayload.setStock(20);
        requestPayload.setStatus("ON_SALE");
        requestPayload.setMainImage("https://example.com/image.png");
        Category categoryReference = new Category();
        categoryReference.setId(category.getId());
        requestPayload.setCategory(categoryReference);

        CustomUserDetails currentUser = new CustomUserDetails(
                supplier.getId(),
                supplier.getUsername(),
                supplier.getPassword(),
                supplier.getEmail(),
                supplier.getPhone(),
                "supplier",
                true,
                List.of(() -> "ROLE_SUPPLIER")
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                currentUser,
                currentUser.getPassword(),
                currentUser.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            ResponseEntity<?> creationResponse = productController.createProduct(requestPayload, currentUser);
            assertEquals(HttpStatus.CREATED, creationResponse.getStatusCode());
            Object responseBody = creationResponse.getBody();
            assertNotNull(responseBody);
            ProductSummaryDTO createdSummary = assertInstanceOf(ProductSummaryDTO.class, responseBody);
            assertNotNull(createdSummary.getId());
            assertEquals(supplier.getId(), createdSummary.getSupplierId());
            assertEquals(category.getId(), createdSummary.getCategoryId());
            assertEquals(new BigDecimal("199.90"), createdSummary.getPrice());
            assertEquals(20, createdSummary.getStock());

            Page<Product> supplierProducts = productService.findBySupplierId(supplier.getId(), PageRequest.of(0, 10));
            assertEquals(1, supplierProducts.getTotalElements());
            Product persisted = supplierProducts.getContent().get(0);
            assertEquals(createdSummary.getId(), persisted.getId());
            assertNotNull(persisted.getSupplier());
            assertEquals(supplier.getId(), persisted.getSupplier().getId());

            ResponseEntity<?> detailResponse = productController.getProductById(createdSummary.getId());
            assertEquals(HttpStatus.OK, detailResponse.getStatusCode());
            ProductDetailDTO detail = assertInstanceOf(ProductDetailDTO.class, detailResponse.getBody());
            assertEquals(createdSummary.getId(), detail.getId());
            assertEquals(createdSummary.getPrice(), detail.getPrice());
            assertNotNull(detail.getSupplier());
            assertEquals(supplier.getCompanyName(), detail.getSupplier().getCompanyName());
            assertEquals(supplier.getSupplierLevel(), detail.getSupplier().getSupplierLevel());
            assertNotNull(detail.getCategory());
            assertEquals(category.getId(), detail.getCategory().getId());
            assertEquals(category.getName(), detail.getCategory().getName());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
