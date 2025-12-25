package com.example.silkmall.service.impl;

import com.example.silkmall.dto.ProductOverviewDTO;
import com.example.silkmall.entity.Product;
import com.example.silkmall.entity.ProductSizeAllocation;
import com.example.silkmall.repository.ProductRepository;
import com.example.silkmall.repository.ProductSizeAllocationRepository;
import com.example.silkmall.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl extends BaseServiceImpl<Product, Long> implements ProductService {
    private final ProductRepository productRepository;
    private final ProductSizeAllocationRepository productSizeAllocationRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository,
                              ProductSizeAllocationRepository productSizeAllocationRepository) {
        super(productRepository);
        this.productRepository = productRepository;
        this.productSizeAllocationRepository = productSizeAllocationRepository;
    }
    
    @Override
    public Page<Product> findByStatus(String status, Pageable pageable) {
        return productRepository.findByStatus(status, pageable);
    }
    
    @Override
    public Page<Product> findByCategoryId(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable);
    }
    
    @Override
    public Page<Product> findBySupplierId(Long supplierId, Pageable pageable) {
        Pageable effectivePageable = pageable;
        if (effectivePageable == null || effectivePageable.getSort().isUnsorted()) {
            int pageNumber = effectivePageable != null ? effectivePageable.getPageNumber() : 0;
            int pageSize = effectivePageable != null ? effectivePageable.getPageSize() : 20;
            effectivePageable = PageRequest.of(
                    Math.max(pageNumber, 0),
                    Math.max(pageSize, 1),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return productRepository.findBySupplierId(supplierId, effectivePageable);
    }
    
    @Override
    public List<Product> findTop10ByOrderBySalesDesc() {
        return productRepository.findTop10ByOrderBySalesDesc();
    }

    @Override
    public List<Product> findTop8ByStatusOrderByCreatedAtDesc(String status) {
        return productRepository.findTop8ByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    public List<Product> findTop8ByStatusOrderBySalesDesc(String status) {
        return productRepository.findTop8ByStatusOrderBySalesDesc(status);
    }

    @Override
    public List<Product> findTop8ByStatusOrderByPriceAsc(String status) {
        return productRepository.findTop8ByStatusOrderByPriceAsc(status);
    }
    
    @Override
    public Page<Product> search(String keyword, Pageable pageable) {
        return productRepository.findByNameContaining(keyword, pageable);
    }

    @Override
    public Page<Product> advancedSearch(String keyword,
                                        Long categoryId,
                                        Long supplierId,
                                        BigDecimal minPrice,
                                        BigDecimal maxPrice,
                                        String status,
                                        Pageable pageable) {
        Specification<Product> specification = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), lowerKeyword),
                    cb.like(cb.lower(root.get("description")), lowerKeyword)
            ));
        }

        if (categoryId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId));
        }

        if (supplierId != null) {
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("supplier").get("id"), supplierId));
        }

        if (minPrice != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }

        if (maxPrice != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }

        if (status != null && !status.isBlank()) {
            String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
            specification = specification.and((root, query, cb) ->
                    cb.equal(root.get("status"), normalizedStatus));
        }

        return productRepository.findAll(specification, pageable);
    }

    @Transactional
    @Override
    public void updateStock(Long id, Integer quantity) {
        Product product = findById(id)
                .orElseThrow(() -> new RuntimeException("产品不存在"));
        
        int newStock = product.getStock() + quantity;
        if (newStock < 0) {
            throw new RuntimeException("库存不足");
        }
        
        product.setStock(newStock);
        productRepository.save(product);
    }
    
    @Transactional
    @Override
    public void increaseSales(Long id, Integer quantity) {
        Product product = findById(id)
                .orElseThrow(() -> new RuntimeException("产品不存在"));
        
        product.setSales(product.getSales() + quantity);
        productRepository.save(product);
    }
    
    @Override
    public void putProductOnSale(Long id) {
        Product product = findById(id)
                .orElseThrow(() -> new RuntimeException("产品不存在"));
        
        if (product.getStock() <= 0) {
            throw new RuntimeException("产品库存为0，无法上架");
        }
        
        product.setStatus("ON_SALE");
        productRepository.save(product);
    }
    
    @Override
    public void takeProductOffSale(Long id) {
        Product product = findById(id)
                .orElseThrow(() -> new RuntimeException("产品不存在"));
        
        product.setStatus("OFF_SALE");
        productRepository.save(product);
    }
    
    @Override
    public Product save(Product product) {
        if (product.getId() != null && product.getCreatedAt() == null) {
            productRepository.findById(product.getId())
                    .map(Product::getCreatedAt)
                    .ifPresent(product::setCreatedAt);
        }
        // 初始化销量为0
        if (product.getSales() == null) {
            product.setSales(0);
        }
        // 初始化库存为0
        if (product.getStock() == null) {
            product.setStock(0);
        }
        // 默认状态为下架
        if (product.getStatus() == null) {
            product.setStatus("OFF_SALE");
        }

        if (product.getUnit() != null) {
            String trimmedUnit = product.getUnit().trim();
            product.setUnit(trimmedUnit.isEmpty() ? null : trimmedUnit);
        }

        Product persisted = super.save(product);
        syncSizeAllocations(persisted, product.getSizeQuantities());
        return withSizeAllocations(persisted);
    }

    @Override
    public ProductOverviewDTO getProductOverview() {
        ProductOverviewDTO overview = new ProductOverviewDTO();
        overview.setTotalProducts(productRepository.count());
        overview.setOnSaleProducts(productRepository.countByStatus("ON_SALE"));
        overview.setOffSaleProducts(productRepository.countByStatus("OFF_SALE"));
        overview.setSoldOutProducts(productRepository.countByStockLessThanEqual(0));

        Long totalStock = productRepository.sumStock();
        overview.setTotalStock(totalStock != null ? totalStock : 0L);

        Long totalSalesVolume = productRepository.sumSales();
        overview.setTotalSalesVolume(totalSalesVolume != null ? totalSalesVolume : 0L);

        return overview;
    }

    @Override
    public Product withSizeAllocations(Product product) {
        if (product == null || product.getId() == null) {
            return product;
        }
        List<ProductSizeAllocation> allocations = productSizeAllocationRepository.findByProductId(product.getId());
        product.setSizeAllocations(allocations);
        Map<String, Integer> asMap = allocations.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        ProductSizeAllocation::getSizeLabel,
                        a -> a.getQuantity() == null ? 0 : a.getQuantity(),
                        Integer::sum));
        product.setSizeQuantities(asMap);
        return product;
    }

    private void syncSizeAllocations(Product product, Map<String, Integer> sizeQuantities) {
        if (product == null || product.getId() == null) {
            return;
        }
        productSizeAllocationRepository.deleteByProductId(product.getId());
        if (sizeQuantities == null || sizeQuantities.isEmpty()) {
            product.setSizeQuantities(Map.of());
            product.setSizeAllocations(List.of());
            return;
        }
        int total = sizeQuantities.values().stream()
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        if (total != product.getStock()) {
            throw new RuntimeException("尺码分配数量与库存不一致");
        }
        List<ProductSizeAllocation> allocations = sizeQuantities.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .map(entry -> {
                    ProductSizeAllocation allocation = new ProductSizeAllocation();
                    allocation.setProduct(product);
                    allocation.setSizeLabel(entry.getKey());
                    allocation.setQuantity(entry.getValue());
                    return allocation;
                })
                .toList();
        productSizeAllocationRepository.saveAll(allocations);
        product.setSizeAllocations(allocations);
        product.setSizeQuantities(
                allocations.stream().collect(Collectors.toMap(ProductSizeAllocation::getSizeLabel, ProductSizeAllocation::getQuantity)));
    }
}
