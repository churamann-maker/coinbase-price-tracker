package com.tracker.service;

import com.tracker.exception.RecommendationNotAvailableException;
import com.tracker.exception.SymbolNotEnrolledException;
import com.tracker.model.RecommendationResponse;
import com.tracker.model.RecommendationResponse.RecommendationType;
import com.tracker.model.RecommendationResponse.SentimentData;
import com.tracker.model.SentimentResult;
import com.tracker.model.TrendData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final EnrollmentService enrollmentService;
    private final SentimentAnalysisService sentimentService;
    private final PriceHistoryService priceHistoryService;

    public RecommendationResponse getRecommendation(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);

        if (!enrollmentService.isEnrolled(normalizedSymbol)) {
            throw new SymbolNotEnrolledException(symbol);
        }

        if (!enrollmentService.isRecommendationAvailable(normalizedSymbol)) {
            Instant availableAt = enrollmentService.getRecommendationAvailableDate(normalizedSymbol);
            long daysRemaining = enrollmentService.getDaysUntilRecommendation(normalizedSymbol);
            throw new RecommendationNotAvailableException(symbol, availableAt, daysRemaining);
        }

        priceHistoryService.recordPrice(normalizedSymbol);

        SentimentResult sentimentResult = sentimentService.analyzeSentiment(normalizedSymbol);
        TrendData trendData = priceHistoryService.analyzeTrend(normalizedSymbol);

        RecommendationType recommendation = determineRecommendation(sentimentResult, trendData);
        String reasoning = buildReasoning(sentimentResult, trendData, recommendation);

        String[] parts = normalizedSymbol.split("-");
        String baseSymbol = parts[0];
        String currency = parts.length > 1 ? parts[1] : "USD";

        log.info("Recommendation for {}: {} - {}", normalizedSymbol, recommendation, reasoning);

        return RecommendationResponse.builder()
                .symbol(baseSymbol)
                .currency(currency)
                .recommendation(recommendation)
                .reasoning(reasoning)
                .sentiment(SentimentData.builder()
                        .overallSentiment(sentimentResult.getOverallSentiment())
                        .positiveScore(sentimentResult.getPositiveScore())
                        .negativeScore(sentimentResult.getNegativeScore())
                        .neutralScore(sentimentResult.getNeutralScore())
                        .tweetsAnalyzed(sentimentResult.getTweetsAnalyzed())
                        .build())
                .trend(trendData)
                .timestamp(Instant.now())
                .build();
    }

    private RecommendationType determineRecommendation(SentimentResult sentiment, TrendData trend) {
        boolean isPositiveSentiment = sentiment.isPositive();
        boolean isTrendingUp = trend.isTrendingUpwards();

        if (isPositiveSentiment && isTrendingUp) {
            return RecommendationType.BUY;
        } else if (!isPositiveSentiment) {
            return RecommendationType.SELL;
        } else {
            return RecommendationType.HOLD;
        }
    }

    private String buildReasoning(SentimentResult sentiment, TrendData trend, RecommendationType recommendation) {
        StringBuilder reasoning = new StringBuilder();

        switch (recommendation) {
            case BUY -> {
                reasoning.append("Positive market sentiment detected");
                if (sentiment.getTweetsAnalyzed() > 0) {
                    reasoning.append(String.format(" (%.0f%% positive from %d tweets)",
                            sentiment.getPositiveScore() * 100, sentiment.getTweetsAnalyzed()));
                }
                reasoning.append(" with upward price trend");
                if (trend.getPercentAboveAverage() != 0) {
                    reasoning.append(String.format(" (%.2f%% above 7-day moving average)", trend.getPercentAboveAverage()));
                }
                reasoning.append(".");
            }
            case SELL -> {
                reasoning.append("Market sentiment is not positive");
                if (sentiment.getTweetsAnalyzed() > 0) {
                    reasoning.append(String.format(" (%s sentiment with %.0f%% negative from %d tweets)",
                            sentiment.getOverallSentiment().toLowerCase(),
                            sentiment.getNegativeScore() * 100,
                            sentiment.getTweetsAnalyzed()));
                }
                reasoning.append(". Consider selling or avoiding new positions.");
            }
            case HOLD -> {
                reasoning.append("Mixed signals detected. ");
                if (sentiment.isPositive()) {
                    reasoning.append("Sentiment is positive but ");
                } else {
                    reasoning.append("Sentiment is ");
                    reasoning.append(sentiment.getOverallSentiment().toLowerCase());
                    reasoning.append(" and ");
                }
                if (trend.isTrendingUpwards()) {
                    reasoning.append("price is trending upwards. ");
                } else {
                    reasoning.append("price is below 7-day moving average. ");
                }
                reasoning.append("Consider holding current position.");
            }
        }

        return reasoning.toString();
    }

    private String normalizeSymbol(String symbol) {
        if (symbol.contains("-")) {
            return symbol.toUpperCase();
        }
        return symbol.toUpperCase() + "-USD";
    }
}
