package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.ConsumerFavorite;
import com.example.silkmall.entity.Product;
import com.example.silkmall.repository.ConsumerFavoriteRepository;
import com.example.silkmall.repository.ConsumerRepository;
import com.example.silkmall.repository.ProductRepository;
import com.example.silkmall.service.ConsumerFavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsumerFavoriteServiceImpl extends BaseServiceImpl<ConsumerFavorite, Long> implements ConsumerFavoriteService {

    private final ConsumerFavoriteRepository favoriteRepository;
    private final ConsumerRepository consumerRepository;
    private final ProductRepository productRepository;

    public ConsumerFavoriteServiceImpl(ConsumerFavoriteRepository favoriteRepository,
                                       ConsumerRepository consumerRepository,
                                       ProductRepository productRepository) {
        super(favoriteRepository);
        this.favoriteRepository = favoriteRepository;
        this.consumerRepository = consumerRepository;
        this.productRepository = productRepository;
    }

    @Override
    public List<ConsumerFavorite> listFavorites(Long consumerId) {
        return favoriteRepository.findByConsumerIdOrderByCreatedAtDesc(consumerId);
    }

    @Override
    @Transactional
    public ConsumerFavorite addFavorite(Long consumerId, Long productId) {
        Consumer consumer = consumerRepository.findById(consumerId)
                .orElseThrow(() -> new RuntimeException("消费者不存在"));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        return favoriteRepository.findByConsumerIdAndProductId(consumerId, productId)
                .orElseGet(() -> {
                    ConsumerFavorite favorite = new ConsumerFavorite();
                    favorite.setConsumer(consumer);
                    favorite.setProduct(product);
                    return favoriteRepository.save(favorite);
                });
    }

    @Override
    @Transactional
    public void removeFavorite(Long consumerId, Long favoriteId) {
        ConsumerFavorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new RuntimeException("收藏记录不存在"));

        if (!favorite.getConsumer().getId().equals(consumerId)) {
            throw new RuntimeException("无权操作该收藏记录");
        }

        favoriteRepository.delete(favorite);
    }
}
