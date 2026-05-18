package com.clothingstore.clothing_store_be.repository;

import com.clothingstore.clothing_store_be.entity.OrderItem;
import com.clothingstore.clothing_store_be.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

       List<OrderItem> findByOrderId(Long orderId);

       boolean existsByVariantId(String variantId);

       // Xác nhận: order item tồn tại, thuộc user, và đơn hàng đã hoàn thành
       @Query("SELECT oi FROM OrderItem oi WHERE oi.id = :itemId " +
                     "AND oi.order.user.userId = :userId " +
                     "AND oi.order.status = 'completed'")
       Optional<OrderItem> findCompletedItemByIdAndUser(
                     @Param("itemId") Long itemId,
                     @Param("userId") Long userId);

       @Query("SELECT oi FROM OrderItem oi " +
                     "LEFT JOIN Review r ON r.orderItem = oi " +
                     "WHERE oi.order.user.userId = :userId " +
                     "AND oi.order.status = 'completed' " +
                     "AND r.id IS NULL " +
                     "ORDER BY oi.order.updatedAt DESC, oi.id DESC")
       List<OrderItem> findCompletedItemsWithoutReviewByUserId(@Param("userId") Long userId);

       @Query("SELECT p.id, p.name, p.thumbnailUrl, c.name, SUM(oi.quantity), SUM(oi.lineTotal) " +
                     "FROM OrderItem oi " +
                     "JOIN oi.order o " +
                     "JOIN Invoice i ON i.order = o " +
                     "JOIN oi.variant v " +
                     "JOIN v.product p " +
                     "JOIN p.category c " +
                     "WHERE i.status = :status AND i.issuedDate BETWEEN :startDate AND :endDate " +
                     "GROUP BY p.id, p.name, p.thumbnailUrl, c.name " +
                     "ORDER BY SUM(oi.quantity) DESC")
       Page<Object[]> findTopSellingProducts(
                     @Param("status") InvoiceStatus status,
                     @Param("startDate") LocalDate startDate,
                     @Param("endDate") LocalDate endDate,
                     Pageable pageable);
}
