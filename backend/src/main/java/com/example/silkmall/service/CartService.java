package com.example.silkmall.service;

import com.example.silkmall.entity.CartItem;

import java.util.List;

public interface CartService extends BaseService<CartItem, Long> {
    List<CartItem> getCartItems(Long consumerId);

    CartItem addItem(Long consumerId, Long productId, int quantity);

    CartItem updateQuantity(Long consumerId, Long itemId, int quantity);

    void removeItem(Long consumerId, Long itemId);

    void clearCart(Long consumerId);
}

