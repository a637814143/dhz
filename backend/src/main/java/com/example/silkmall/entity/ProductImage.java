package com.example.silkmall.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "product_images")
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String imageUrl;
    private Integer sortOrder;
    private Date createdAt;
    
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}