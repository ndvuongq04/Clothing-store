package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.address.AddressCreateRequest;
import com.clothingstore.clothing_store_be.dto.address.AddressDto;
import com.clothingstore.clothing_store_be.dto.address.AddressUpdateRequest;

import java.util.List;

public interface AddressService {

    List<AddressDto> getAll(Long userId);

    AddressDto create(Long userId, AddressCreateRequest req);

    AddressDto update(Long userId, Long addressId, AddressUpdateRequest req);

    void delete(Long userId, Long addressId);

    AddressDto setDefault(Long userId, Long addressId);
}
