package com.clothingstore.clothing_store_be.dto.statistic;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderChartData {
    private String label;
    private long completed;
    private long cancelled;
}
