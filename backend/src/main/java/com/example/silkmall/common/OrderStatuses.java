package com.example.silkmall.common;

public final class OrderStatuses {
    public static final String PENDING_PAYMENT = "待付款";
    public static final String PENDING_SHIPMENT = "待发货";
    public static final String SHIPPED = "已发货";
    public static final String IN_TRANSIT = "运送中";
    public static final String AWAITING_RECEIPT = "待收货";
    public static final String DELIVERED = "已收货";
    public static final String CANCELLED = "已取消";
    public static final String REVOKED = "已撤销";

    private OrderStatuses() {
    }
}
