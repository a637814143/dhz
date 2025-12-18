package com.example.silkmall.repository;

import com.example.silkmall.entity.ConsumerFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsumerFavoriteRepository extends JpaRepository<ConsumerFavorite, Long> {
    List<ConsumerFavorite> findByConsumerIdOrderByCreatedAtDesc(Long consumerId);

    Optional<ConsumerFavorite> findByConsumerIdAndProductId(Long consumerId, Long productId);

    boolean existsByConsumerIdAndProductId(Long consumerId, Long productId);
}
