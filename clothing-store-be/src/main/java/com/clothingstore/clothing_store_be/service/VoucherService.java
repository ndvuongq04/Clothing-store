package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.dto.voucher.VoucherCreateRequest;
import com.clothingstore.clothing_store_be.dto.voucher.VoucherDto;
import com.clothingstore.clothing_store_be.dto.voucher.VoucherUpdateRequest;

public interface VoucherService {

    ResultPaginationDTO getAll(int page, int pageSize);

    // Chỉ trả về voucher active, chưa xóa, còn hạn, còn lượt dùng — dành cho client
    ResultPaginationDTO getAllActive(int page, int pageSize);

    VoucherDto getByCode(String voucherCode);

    VoucherDto create(VoucherCreateRequest req);

    VoucherDto update(String voucherCode, VoucherUpdateRequest req);

    void delete(String voucherCode);

    VoucherDto toggleActive(String voucherCode);
}
