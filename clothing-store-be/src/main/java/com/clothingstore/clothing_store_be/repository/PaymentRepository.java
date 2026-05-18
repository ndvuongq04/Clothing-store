package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByVnpayTxnRef(String txnRef);

    List<Payment> findByMethodAndCreatedAtBetween(String method, LocalDateTime from, LocalDateTime to);
}
