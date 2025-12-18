package com.example.silkmall.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class WeeklySalesBucketDTO {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private int totalOrders;
    private int totalQuantity;
    private BigDecimal totalRevenue;
    private List<WeeklyOrderDTO> orders;
    private List<WeeklyProductPerformanceDTO> productPerformances;

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(LocalDate weekStart) {
        this.weekStart = weekStart;
    }

    public LocalDate getWeekEnd() {
        return weekEnd;
    }

    public void setWeekEnd(LocalDate weekEnd) {
        this.weekEnd = weekEnd;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(int totalOrders) {
        this.totalOrders = totalOrders;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public List<WeeklyOrderDTO> getOrders() {
        return orders;
    }

    public void setOrders(List<WeeklyOrderDTO> orders) {
        this.orders = orders;
    }

    public List<WeeklyProductPerformanceDTO> getProductPerformances() {
        return productPerformances;
    }

    public void setProductPerformances(List<WeeklyProductPerformanceDTO> productPerformances) {
        this.productPerformances = productPerformances;
    }
}
