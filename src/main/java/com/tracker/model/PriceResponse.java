package com.tracker.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PriceResponse {
    private String symbol;
    private String currency;
    private BigDecimal spotPrice;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private Instant timestamp;
}
