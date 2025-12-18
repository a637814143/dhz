package com.example.silkmall.service;

import com.example.silkmall.entity.ConsumerFavorite;

import java.util.List;

public interface ConsumerFavoriteService extends BaseService<ConsumerFavorite, Long> {
    List<ConsumerFavorite> listFavorites(Long consumerId);

    ConsumerFavorite addFavorite(Long consumerId, Long productId);

    void removeFavorite(Long consumerId, Long favoriteId);
}
