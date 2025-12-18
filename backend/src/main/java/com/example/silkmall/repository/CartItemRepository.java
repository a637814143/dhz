package com.example.silkmall.repository;

import com.example.silkmall.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("SELECT c FROM CartItem c WHERE c.consumer.id = :consumerId ORDER BY c.createdAt DESC")
    List<CartItem> findByConsumerIdOrderByCreatedAtDesc(@Param("consumerId") Long consumerId);

    @Query("SELECT c FROM CartItem c WHERE c.consumer.id = :consumerId AND c.product.id = :productId")
    Optional<CartItem> findByConsumerIdAndProductId(@Param("consumerId") Long consumerId,
                                                    @Param("productId") Long productId);

    List<CartItem> findByIdInAndConsumerId(List<Long> ids, Long consumerId);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.consumer.id = :consumerId")
    void deleteByConsumerId(@Param("consumerId") Long consumerId);
}

