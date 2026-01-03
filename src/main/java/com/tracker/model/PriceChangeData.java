package com.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceChangeData {
    private BigDecimal currentPrice;
    private BigDecimal dailyChangePercent;
    private BigDecimal avgChangePercent;
    private int daysOfData;
}
