package com.example.silkmall.dto;

import java.math.BigDecimal;
import java.util.Date;

public class OrderItemDetailDTO {
    private Long id;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private Date createdAt;
    private OrderedProductDTO product;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public OrderedProductDTO getProduct() {
        return product;
    }

    public void setProduct(OrderedProductDTO product) {
        this.product = product;
    }

    public static class OrderedProductDTO {
        private Long id;
        private String name;
        private String mainImage;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getMainImage() {
            return mainImage;
        }

        public void setMainImage(String mainImage) {
            this.mainImage = mainImage;
        }
    }
}
