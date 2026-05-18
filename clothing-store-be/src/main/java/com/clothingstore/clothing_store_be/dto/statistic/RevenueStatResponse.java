package com.clothingstore.clothing_store_be.dto.statistic;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueStatResponse {
    private List<String> labels;
    private List<BigDecimal> revenue;
    private List<BigDecimal> profit;
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;
    private Double growthRate;
}
