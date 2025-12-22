package com.tracker.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class PriceHistory {
    private String symbol;
    private String currency;
    private List<PricePoint> history;
    private int totalRecords;

    @Data
    @Builder
    public static class PricePoint {
        private BigDecimal spotPrice;
        private BigDecimal buyPrice;
        private BigDecimal sellPrice;
        private Instant timestamp;
    }
}
