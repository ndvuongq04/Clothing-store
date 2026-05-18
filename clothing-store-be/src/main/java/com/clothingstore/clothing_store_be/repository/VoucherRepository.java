package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.Voucher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, String> {

    // Dùng khi user áp dụng voucher: phải active và chưa bị xóa mềm
    Optional<Voucher> findByVoucherCodeAndActiveTrueAndDeletedAtIsNull(String voucherCode);

    // Dành cho client: active, chưa xóa, chưa hết hạn, còn lượt dùng
    @Query("SELECT v FROM Voucher v WHERE v.active = true AND v.deletedAt IS NULL " +
            "AND v.expiryDate >= :today AND v.usedCount < v.maxUsage")
    Page<Voucher> findAllActiveForClient(@Param("today") LocalDate today, Pageable pageable);

    Page<Voucher> findAll(Pageable pageable);
}
