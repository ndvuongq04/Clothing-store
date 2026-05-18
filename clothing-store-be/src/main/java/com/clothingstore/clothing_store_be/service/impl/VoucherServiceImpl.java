package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.dto.voucher.VoucherCreateRequest;
import com.clothingstore.clothing_store_be.dto.voucher.VoucherDto;
import com.clothingstore.clothing_store_be.dto.voucher.VoucherUpdateRequest;
import com.clothingstore.clothing_store_be.entity.Voucher;
import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.repository.VoucherRepository;
import com.clothingstore.clothing_store_be.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getAll(int page, int pageSize) {
        Page<Voucher> result = voucherRepository.findAll(
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta(
                result.getNumber(), result.getSize(),
                result.getTotalPages(), result.getTotalElements());

        return new ResultPaginationDTO(meta,
                result.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getAllActive(int page, int pageSize) {
        Page<Voucher> result = voucherRepository.findAllActiveForClient(
                LocalDate.now(),
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta(
                result.getNumber(), result.getSize(),
                result.getTotalPages(), result.getTotalElements());

        return new ResultPaginationDTO(meta,
                result.getContent().stream().map(this::toDto).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherDto getByCode(String voucherCode) {
        return toDto(findOrThrow(voucherCode));
    }

    @Override
    public VoucherDto create(VoucherCreateRequest req) {
        if (voucherRepository.existsById(req.getVoucherCode())) {
            throw AppException.conflict("Mã voucher '" + req.getVoucherCode() + "' đã tồn tại");
        }
        if (req.getExpiryDate().isBefore(req.getStartDate())) {
            throw AppException.badRequest("Ngày hết hạn phải sau ngày bắt đầu");
        }

        Voucher voucher = Voucher.builder()
                .voucherCode(req.getVoucherCode())
                .type(req.getType())
                .discountValue(req.getDiscountValue())
                .minOrderValue(req.getMinOrderValue() != null ? req.getMinOrderValue() : BigDecimal.ZERO)
                .maxDiscountCap(req.getMaxDiscountCap())
                .startDate(req.getStartDate())
                .expiryDate(req.getExpiryDate())
                .maxUsage(req.getMaxUsage())
                .build();

        return toDto(voucherRepository.save(voucher));
    }

    @Override
    public VoucherDto update(String voucherCode, VoucherUpdateRequest req) {
        Voucher voucher = findOrThrow(voucherCode);

        if (req.getType() != null) voucher.setType(req.getType());
        if (req.getDiscountValue() != null) voucher.setDiscountValue(req.getDiscountValue());
        if (req.getMinOrderValue() != null) voucher.setMinOrderValue(req.getMinOrderValue());
        if (req.getMaxDiscountCap() != null) voucher.setMaxDiscountCap(req.getMaxDiscountCap());
        if (req.getStartDate() != null) voucher.setStartDate(req.getStartDate());
        if (req.getExpiryDate() != null) voucher.setExpiryDate(req.getExpiryDate());
        if (req.getMaxUsage() != null) voucher.setMaxUsage(req.getMaxUsage());
        if (req.getActive() != null) voucher.setActive(req.getActive());

        if (voucher.getExpiryDate().isBefore(voucher.getStartDate())) {
            throw AppException.badRequest("Ngày hết hạn phải sau ngày bắt đầu");
        }

        return toDto(voucherRepository.save(voucher));
    }

    @Override
    public void delete(String voucherCode) {
        Voucher voucher = findOrThrow(voucherCode);
        if (voucher.getDeletedAt() != null) {
            throw AppException.conflict("Voucher đã bị xóa trước đó");
        }
        voucher.setDeletedAt(java.time.LocalDateTime.now());
        voucher.setActive(false);
        voucherRepository.save(voucher);
    }

    @Override
    public VoucherDto toggleActive(String voucherCode) {
        Voucher voucher = findOrThrow(voucherCode);
        voucher.setActive(!voucher.isActive());
        return toDto(voucherRepository.save(voucher));
    }

    // ─── Helpers ─────────────────────────────────────────

    private Voucher findOrThrow(String voucherCode) {
        return voucherRepository.findById(voucherCode)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy voucher: " + voucherCode));
    }

    private VoucherDto toDto(Voucher v) {
        return VoucherDto.builder()
                .voucherCode(v.getVoucherCode())
                .type(v.getType())
                .discountValue(v.getDiscountValue())
                .minOrderValue(v.getMinOrderValue())
                .maxDiscountCap(v.getMaxDiscountCap())
                .startDate(v.getStartDate())
                .expiryDate(v.getExpiryDate())
                .maxUsage(v.getMaxUsage())
                .usedCount(v.getUsedCount())
                .active(v.isActive())
                .createdAt(v.getCreatedAt())
                .deletedAt(v.getDeletedAt())
                .build();
    }
}
