package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.dto.address.AddressCreateRequest;
import com.clothingstore.clothing_store_be.dto.address.AddressDto;
import com.clothingstore.clothing_store_be.dto.address.AddressUpdateRequest;
import com.clothingstore.clothing_store_be.entity.Address;
import com.clothingstore.clothing_store_be.entity.User;
import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.repository.AddressRepository;
import com.clothingstore.clothing_store_be.repository.UserRepository;
import com.clothingstore.clothing_store_be.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressDto> getAll(Long userId) {
        return addressRepository
                .findByUserUserIdAndDeletedAtIsNullOrderByIsDefaultDescIdAsc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public AddressDto create(Long userId, AddressCreateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Người dùng không tồn tại"));

        if (req.isDefault()) {
            addressRepository.clearDefaultByUserId(userId);
        }

        Address address = Address.builder()
                .user(user)
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .province(req.getProvince())
                .district(req.getDistrict())
                .ward(req.getWard())
                .street(req.getStreet())
                .isDefault(req.isDefault())
                .build();

        return toDto(addressRepository.save(address));
    }

    @Override
    public AddressDto update(Long userId, Long addressId, AddressUpdateRequest req) {
        Address address = findOrThrow(userId, addressId);

        if (req.getFullName() != null) address.setFullName(req.getFullName());
        if (req.getPhone() != null) address.setPhone(req.getPhone());
        if (req.getProvince() != null) address.setProvince(req.getProvince());
        if (req.getDistrict() != null) address.setDistrict(req.getDistrict());
        if (req.getWard() != null) address.setWard(req.getWard());
        if (req.getStreet() != null) address.setStreet(req.getStreet());

        return toDto(addressRepository.save(address));
    }

    @Override
    public void delete(Long userId, Long addressId) {
        Address address = findOrThrow(userId, addressId);
        address.setDeletedAt(LocalDateTime.now());
        // Nếu xóa địa chỉ mặc định thì bỏ cờ default
        if (address.isDefault()) {
            address.setDefault(false);
        }
        addressRepository.save(address);
    }

    @Override
    public AddressDto setDefault(Long userId, Long addressId) {
        findOrThrow(userId, addressId); // validate ownership trước
        addressRepository.clearDefaultByUserId(userId);
        Address address = findOrThrow(userId, addressId);
        address.setDefault(true);
        return toDto(addressRepository.save(address));
    }

    // ─── Helpers ─────────────────────────────────────────

    private Address findOrThrow(Long userId, Long addressId) {
        return addressRepository.findByIdAndUserUserIdAndDeletedAtIsNull(addressId, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy địa chỉ"));
    }

    private AddressDto toDto(Address a) {
        return AddressDto.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .phone(a.getPhone())
                .province(a.getProvince())
                .district(a.getDistrict())
                .ward(a.getWard())
                .street(a.getStreet())
                .isDefault(a.isDefault())
                .build();
    }
}
