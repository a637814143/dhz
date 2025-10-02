package com.example.silkmall.dto;

/**
 * Dashboard style overview metrics for the product catalogue.
 */
public class ProductOverviewDTO {
    private long totalProducts;
    private long onSaleProducts;
    private long offSaleProducts;
    private long soldOutProducts;
    private long totalStock;
    private long totalSalesVolume;

    public long getTotalProducts() {
        return totalProducts;
    }

    public void setTotalProducts(long totalProducts) {
        this.totalProducts = totalProducts;
    }

    public long getOnSaleProducts() {
        return onSaleProducts;
    }

    public void setOnSaleProducts(long onSaleProducts) {
        this.onSaleProducts = onSaleProducts;
    }

    public long getOffSaleProducts() {
        return offSaleProducts;
    }

    public void setOffSaleProducts(long offSaleProducts) {
        this.offSaleProducts = offSaleProducts;
    }

    public long getSoldOutProducts() {
        return soldOutProducts;
    }

    public void setSoldOutProducts(long soldOutProducts) {
        this.soldOutProducts = soldOutProducts;
    }

    public long getTotalStock() {
        return totalStock;
    }

    public void setTotalStock(long totalStock) {
        this.totalStock = totalStock;
    }

    public long getTotalSalesVolume() {
        return totalSalesVolume;
    }

    public void setTotalSalesVolume(long totalSalesVolume) {
        this.totalSalesVolume = totalSalesVolume;
    }
}