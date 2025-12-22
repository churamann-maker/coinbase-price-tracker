package com.tracker.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class RecommendationResponse {
    private String symbol;
    private String currency;
    private RecommendationType recommendation;
    private String reasoning;
    private SentimentData sentiment;
    private TrendData trend;
    private Instant timestamp;

    public enum RecommendationType {
        BUY,
        SELL,
        HOLD
    }

    @Data
    @Builder
    public static class SentimentData {
        private String overallSentiment;
        private double positiveScore;
        private double negativeScore;
        private double neutralScore;
        private int tweetsAnalyzed;
    }
}
