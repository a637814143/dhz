package com.example.silkmall.service.impl;

import com.example.silkmall.entity.Consumer;
import com.example.silkmall.entity.ConsumerAddress;
import com.example.silkmall.repository.ConsumerAddressRepository;
import com.example.silkmall.service.ConsumerAddressService;
import com.example.silkmall.service.ConsumerService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConsumerAddressServiceImpl implements ConsumerAddressService {
    private final ConsumerAddressRepository consumerAddressRepository;
    private final ConsumerService consumerService;

    public ConsumerAddressServiceImpl(ConsumerAddressRepository consumerAddressRepository, ConsumerService consumerService) {
        this.consumerAddressRepository = consumerAddressRepository;
        this.consumerService = consumerService;
    }

    @Override
    public List<ConsumerAddress> listAddresses(Long consumerId) {
        return consumerAddressRepository.findByConsumerIdOrderByIsDefaultDescUpdatedAtDesc(consumerId);
    }

    @Override
    @Transactional
    public ConsumerAddress createAddress(Long consumerId, ConsumerAddress address) {
        Consumer consumer = consumerService.findById(consumerId)
                .orElseThrow(() -> new IllegalArgumentException("消费者不存在"));
        address.setId(null);
        address.setConsumer(consumer);
        boolean shouldBeDefault = Boolean.TRUE.equals(address.getIsDefault())
                || !consumerAddressRepository.existsByConsumerIdAndIsDefaultTrue(consumerId);
        address.setIsDefault(shouldBeDefault);
        ConsumerAddress saved = consumerAddressRepository.save(address);
        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            consumerAddressRepository.clearDefaultExcept(consumerId, saved.getId());
        }
        return saved;
    }

    @Override
    @Transactional
    public ConsumerAddress updateAddress(Long consumerId, Long addressId, ConsumerAddress payload) {
        ConsumerAddress existing = consumerAddressRepository.findByIdAndConsumerId(addressId, consumerId)
                .orElseThrow(() -> new IllegalArgumentException("地址不存在"));
        existing.setRecipientName(payload.getRecipientName());
        existing.setRecipientPhone(payload.getRecipientPhone());
        existing.setShippingAddress(payload.getShippingAddress());
        existing.setIsDefault(Boolean.TRUE.equals(payload.getIsDefault()));
        ConsumerAddress saved = consumerAddressRepository.save(existing);
        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            consumerAddressRepository.clearDefaultExcept(consumerId, saved.getId());
        } else {
            ensureDefaultAddress(consumerId);
        }
        return saved;
    }

    @Override
    @Transactional
    public void deleteAddress(Long consumerId, Long addressId) {
        ConsumerAddress existing = consumerAddressRepository.findByIdAndConsumerId(addressId, consumerId)
                .orElseThrow(() -> new IllegalArgumentException("地址不存在"));
        consumerAddressRepository.delete(existing);
        ensureDefaultAddress(consumerId);
    }

    @Override
    @Transactional
    public ConsumerAddress markDefault(Long consumerId, Long addressId) {
        ConsumerAddress existing = consumerAddressRepository.findByIdAndConsumerId(addressId, consumerId)
                .orElseThrow(() -> new IllegalArgumentException("地址不存在"));
        existing.setIsDefault(true);
        ConsumerAddress saved = consumerAddressRepository.save(existing);
        consumerAddressRepository.clearDefaultExcept(consumerId, saved.getId());
        return saved;
    }

    private void ensureDefaultAddress(Long consumerId) {
        if (consumerAddressRepository.countByConsumerId(consumerId) == 0) {
            return;
        }
        if (consumerAddressRepository.existsByConsumerIdAndIsDefaultTrue(consumerId)) {
            return;
        }
        consumerAddressRepository.findFirstByConsumerIdOrderByIsDefaultDescUpdatedAtDesc(consumerId)
                .ifPresent(address -> {
                    address.setIsDefault(true);
                    ConsumerAddress saved = consumerAddressRepository.save(address);
                    consumerAddressRepository.clearDefaultExcept(consumerId, saved.getId());
                });
    }
}
