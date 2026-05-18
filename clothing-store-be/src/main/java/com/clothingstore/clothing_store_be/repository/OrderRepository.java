package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderCode(String orderCode);

    Optional<Order> findByIdAndUserUserId(Long id, Long userId);

    Page<Order> findByUserUserId(Long userId, Pageable pageable);

    Page<Order> findByUserUserIdAndStatus(Long userId, String status, Pageable pageable);

    long countByUserUserId(Long userId);

    long countByUserUserIdAndStatus(Long userId, String status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.user.userId = :userId AND o.status = 'completed'")
    BigDecimal sumTotalByUserId(@Param("userId") Long userId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
