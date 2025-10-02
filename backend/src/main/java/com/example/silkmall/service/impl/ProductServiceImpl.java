package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Product;
import com.example.silkmall.repository.ProductRepository;
import com.example.silkmall.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ProductServiceImpl extends BaseServiceImpl<Product, Long> implements ProductService {
    private final ProductRepository productRepository;
    
    @Autowired
    public ProductServiceImpl(ProductRepository productRepository) {
        super(productRepository);
        this.productRepository = productRepository;
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
        return productRepository.findBySupplierId(supplierId, pageable);
    }
    
    @Override
    public List<Product> findTop10ByOrderBySalesDesc() {
        return productRepository.findTop10ByOrderBySalesDesc();
    }
    
    @Override
    public Page<Product> search(String keyword, Pageable pageable) {
        return productRepository.findByNameContaining(keyword, pageable);
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
        
        return super.save(product);
    }
}