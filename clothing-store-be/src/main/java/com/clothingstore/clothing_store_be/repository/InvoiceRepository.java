package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.Invoice;
import com.clothingstore.clothing_store_be.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);

    Optional<Invoice> findByInvoiceCode(String invoiceCode);

    long countByStatus(InvoiceStatus status);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status = :status AND i.issuedDate = :date")
    BigDecimal sumTotalAmountByStatusAndIssuedDate(@Param("status") InvoiceStatus status, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(i.totalAmount), 0) FROM Invoice i WHERE i.status = :status AND i.issuedDate BETWEEN :startDate AND :endDate")
    BigDecimal sumTotalAmountByStatusAndIssuedDateBetween(@Param("status") InvoiceStatus status, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<Invoice> findByStatusAndIssuedDateBetween(InvoiceStatus status, LocalDate startDate, LocalDate endDate);
}
