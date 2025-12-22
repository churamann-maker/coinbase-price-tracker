package com.tracker.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SentimentResult {
    private String overallSentiment;
    private double positiveScore;
    private double negativeScore;
    private double neutralScore;
    private double mixedScore;
    private int tweetsAnalyzed;
    private List<TweetSentiment> individualResults;

    public boolean isPositive() {
        return "POSITIVE".equals(overallSentiment);
    }

    @Data
    @Builder
    public static class TweetSentiment {
        private String tweetId;
        private String text;
        private String sentiment;
        private double confidence;
    }
}
