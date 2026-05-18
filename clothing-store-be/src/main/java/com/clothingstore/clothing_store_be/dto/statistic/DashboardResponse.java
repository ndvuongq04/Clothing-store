package com.clothingstore.clothing_store_be.dto.statistic;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardResponse {
    private BigDecimal revenueToday;
    private BigDecimal revenueThisMonth;
    private Long newOrdersToday;
    private Long newCustomersThisMonth;
    private Long lowStockProducts;
}
