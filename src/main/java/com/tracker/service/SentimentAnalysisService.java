package com.tracker.service;

import com.tracker.model.SentimentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.BatchDetectSentimentRequest;
import software.amazon.awssdk.services.comprehend.model.BatchDetectSentimentResponse;
import software.amazon.awssdk.services.comprehend.model.BatchDetectSentimentItemResult;
import software.amazon.awssdk.services.comprehend.model.SentimentScore;
import software.amazon.awssdk.services.comprehend.model.SentimentType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SentimentAnalysisService {

    private final ComprehendClient comprehendClient;
    private final TwitterService twitterService;

    @Value("${recommendation.sentiment.min-tweets-required:10}")
    private int minTweetsRequired;

    @Value("${recommendation.sentiment.positive-threshold:0.5}")
    private double positiveThreshold;

    private static final int MAX_BATCH_SIZE = 25;
    private static final int MAX_TEXT_LENGTH = 5000;

    public SentimentResult analyzeSentiment(String symbol) {
        List<String> tweets = twitterService.getTweetTexts(symbol);

        if (tweets.isEmpty()) {
            log.warn("No tweets available for sentiment analysis of {}", symbol);
            return SentimentResult.builder()
                    .overallSentiment("UNKNOWN")
                    .positiveScore(0.0)
                    .negativeScore(0.0)
                    .neutralScore(0.0)
                    .mixedScore(0.0)
                    .tweetsAnalyzed(0)
                    .individualResults(new ArrayList<>())
                    .build();
        }

        List<String> validTweets = tweets.stream()
                .filter(text -> text != null && text.length() >= 10)
                .map(text -> text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text)
                .collect(Collectors.toList());

        if (validTweets.size() < minTweetsRequired) {
            log.warn("Insufficient tweets for reliable analysis. Found {}, required {}", validTweets.size(), minTweetsRequired);
        }

        List<SentimentResult.TweetSentiment> allResults = new ArrayList<>();
        double totalPositive = 0;
        double totalNegative = 0;
        double totalNeutral = 0;
        double totalMixed = 0;
        int analyzedCount = 0;

        for (int i = 0; i < validTweets.size(); i += MAX_BATCH_SIZE) {
            List<String> batch = validTweets.subList(i, Math.min(i + MAX_BATCH_SIZE, validTweets.size()));

            try {
                BatchDetectSentimentRequest request = BatchDetectSentimentRequest.builder()
                        .textList(batch)
                        .languageCode("en")
                        .build();

                BatchDetectSentimentResponse response = comprehendClient.batchDetectSentiment(request);

                for (BatchDetectSentimentItemResult result : response.resultList()) {
                    SentimentScore score = result.sentimentScore();
                    String tweetText = batch.get(result.index());

                    totalPositive += score.positive();
                    totalNegative += score.negative();
                    totalNeutral += score.neutral();
                    totalMixed += score.mixed();
                    analyzedCount++;

                    allResults.add(SentimentResult.TweetSentiment.builder()
                            .tweetId(String.valueOf(i + result.index()))
                            .text(truncateForDisplay(tweetText))
                            .sentiment(result.sentiment().toString())
                            .confidence(getConfidence(score, result.sentiment()))
                            .build());
                }

                if (!response.errorList().isEmpty()) {
                    log.warn("Some tweets failed sentiment analysis: {}", response.errorList().size());
                }
            } catch (Exception e) {
                log.error("Failed to analyze batch starting at index {}: {}", i, e.getMessage());
            }
        }

        if (analyzedCount == 0) {
            return SentimentResult.builder()
                    .overallSentiment("UNKNOWN")
                    .positiveScore(0.0)
                    .negativeScore(0.0)
                    .neutralScore(0.0)
                    .mixedScore(0.0)
                    .tweetsAnalyzed(0)
                    .individualResults(allResults)
                    .build();
        }

        double avgPositive = totalPositive / analyzedCount;
        double avgNegative = totalNegative / analyzedCount;
        double avgNeutral = totalNeutral / analyzedCount;
        double avgMixed = totalMixed / analyzedCount;

        String overallSentiment = determineOverallSentiment(avgPositive, avgNegative, avgNeutral, avgMixed);

        log.info("Sentiment analysis for {}: {} (positive={:.2f}, negative={:.2f}, tweets={})",
                symbol, overallSentiment, avgPositive, avgNegative, analyzedCount);

        return SentimentResult.builder()
                .overallSentiment(overallSentiment)
                .positiveScore(avgPositive)
                .negativeScore(avgNegative)
                .neutralScore(avgNeutral)
                .mixedScore(avgMixed)
                .tweetsAnalyzed(analyzedCount)
                .individualResults(allResults)
                .build();
    }

    public boolean isPositiveSentiment(String symbol) {
        SentimentResult result = analyzeSentiment(symbol);
        return result.isPositive();
    }

    private String determineOverallSentiment(double positive, double negative, double neutral, double mixed) {
        if (positive >= positiveThreshold && positive > negative) {
            return "POSITIVE";
        } else if (negative > positive && negative > neutral) {
            return "NEGATIVE";
        } else if (mixed > 0.3) {
            return "MIXED";
        } else {
            return "NEUTRAL";
        }
    }

    private double getConfidence(SentimentScore score, SentimentType sentiment) {
        return switch (sentiment) {
            case POSITIVE -> score.positive();
            case NEGATIVE -> score.negative();
            case NEUTRAL -> score.neutral();
            case MIXED -> score.mixed();
            default -> 0.0;
        };
    }

    private String truncateForDisplay(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
