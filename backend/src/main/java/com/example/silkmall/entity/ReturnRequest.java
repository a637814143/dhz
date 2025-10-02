package com.example.silkmall.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;

@Data
@Entity
@Table(name = "return_requests")
public class ReturnRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String status; // PENDING, APPROVED, REJECTED, COMPLETED

    @Column(length = 1000)
    private String reason;

    @Column(length = 1000)
    private String resolution;

    private Date requestedAt;

    private Date processedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id")
    @JsonIgnore
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumer_id")
    private Consumer consumer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @PrePersist
    protected void onCreate() {
        requestedAt = new Date();
    }
}
