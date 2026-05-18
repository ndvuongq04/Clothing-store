package com.clothingstore.clothing_store_be.dto.customer;

import com.clothingstore.clothing_store_be.dto.order.OrderSummaryDto;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CustomerDetailDto {
    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String gender;
    private LocalDate dateOfBirth;
    private boolean status;
    private boolean emailVerified;

    // Thống kê đơn hàng
    private long totalOrders;
    private BigDecimal totalSpent;
    private long completedOrders;
    private long cancelledOrders;

    // 5 đơn gần nhất
    private List<OrderSummaryDto> recentOrders;
}
