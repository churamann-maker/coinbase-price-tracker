package com.tracker.exception;

import lombok.Getter;

import java.time.Instant;

@Getter
public class RecommendationNotAvailableException extends RuntimeException {

    private final String symbol;
    private final Instant availableAt;
    private final long daysRemaining;

    public RecommendationNotAvailableException(String symbol, Instant availableAt, long daysRemaining) {
        super(String.format("Recommendation not yet available for %s. Available in %d day(s).", symbol, daysRemaining));
        this.symbol = symbol;
        this.availableAt = availableAt;
        this.daysRemaining = daysRemaining;
    }
}
