package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.PaymentSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentSettingRepository extends JpaRepository<PaymentSetting, String> {
}
