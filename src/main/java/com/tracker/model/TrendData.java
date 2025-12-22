package com.tracker.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TrendData {
    private BigDecimal currentPrice;
    private BigDecimal sevenDayMovingAverage;
    private boolean trendingUpwards;
    private double percentAboveAverage;
}
