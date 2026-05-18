package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserUserIdAndDeletedAtIsNullOrderByIsDefaultDescIdAsc(Long userId);

    Optional<Address> findByIdAndUserUserIdAndDeletedAtIsNull(Long id, Long userId);

    // Bỏ default của tất cả địa chỉ khác trước khi set default mới
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.userId = :userId AND a.deletedAt IS NULL")
    void clearDefaultByUserId(@Param("userId") Long userId);
}
