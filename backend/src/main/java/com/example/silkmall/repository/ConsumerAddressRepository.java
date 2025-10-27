package com.example.silkmall.repository;

import com.example.silkmall.entity.ConsumerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConsumerAddressRepository extends JpaRepository<ConsumerAddress, Long> {
    List<ConsumerAddress> findByConsumerIdOrderByIsDefaultDescUpdatedAtDesc(Long consumerId);

    Optional<ConsumerAddress> findByIdAndConsumerId(Long id, Long consumerId);

    Optional<ConsumerAddress> findFirstByConsumerIdOrderByIsDefaultDescUpdatedAtDesc(Long consumerId);

    long countByConsumerId(Long consumerId);

    boolean existsByConsumerIdAndIsDefaultTrue(Long consumerId);

    @Modifying
    @Query("update ConsumerAddress a set a.isDefault = false where a.consumer.id = :consumerId and (:excludeId is null or a.id <> :excludeId)")
    void clearDefaultExcept(@Param("consumerId") Long consumerId, @Param("excludeId") Long excludeId);
}
