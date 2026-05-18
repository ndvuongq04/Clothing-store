package com.clothingstore.clothing_store_be.dto.statistic;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatResponse {
    private long total;
    private long completed;
    private long cancelled;
    private long delivering;
    private double completionRate;
    private List<OrderChartData> chartData;
}
