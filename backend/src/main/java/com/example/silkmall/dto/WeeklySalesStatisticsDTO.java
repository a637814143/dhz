package com.example.silkmall.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class WeeklySalesStatisticsDTO {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private Integer orderCount;
    private Integer totalUnitsSold;
    private BigDecimal totalSalesAmount;
    private List<WeeklyOrderDetailDTO> orders;
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

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Integer getTotalUnitsSold() {
        return totalUnitsSold;
    }

    public void setTotalUnitsSold(Integer totalUnitsSold) {
        this.totalUnitsSold = totalUnitsSold;
    }

    public BigDecimal getTotalSalesAmount() {
        return totalSalesAmount;
    }

    public void setTotalSalesAmount(BigDecimal totalSalesAmount) {
        this.totalSalesAmount = totalSalesAmount;
    }

    public List<WeeklyOrderDetailDTO> getOrders() {
        return orders;
    }

    public void setOrders(List<WeeklyOrderDetailDTO> orders) {
        this.orders = orders;
    }

    public List<WeeklyProductPerformanceDTO> getProductPerformances() {
        return productPerformances;
    }

    public void setProductPerformances(List<WeeklyProductPerformanceDTO> productPerformances) {
        this.productPerformances = productPerformances;
    }
}
