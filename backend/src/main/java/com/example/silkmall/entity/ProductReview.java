package com.example.silkmall.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "product_reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"order_item_id"})
})
public class ProductReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer rating;

    @Column(length = 1000)
    private String comment;

    private Date createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id")
    @JsonIgnore
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumer_id")
    private Consumer consumer;

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }
}
