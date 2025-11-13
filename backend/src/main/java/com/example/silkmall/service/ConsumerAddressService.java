package com.example.silkmall.service;

import com.example.silkmall.entity.ConsumerAddress;

import java.util.List;

public interface ConsumerAddressService {
    List<ConsumerAddress> listAddresses(Long consumerId);

    ConsumerAddress createAddress(Long consumerId, ConsumerAddress address);

    ConsumerAddress updateAddress(Long consumerId, Long addressId, ConsumerAddress address);

    void deleteAddress(Long consumerId, Long addressId);

    ConsumerAddress markDefault(Long consumerId, Long addressId);
}
