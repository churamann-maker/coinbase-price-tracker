package com.tracker.service;

import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.endpoints.AdditionalParameters;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.tweet.TweetV2;
import io.github.redouane59.twitter.signature.TwitterCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TwitterService {

    @Value("${twitter.api.bearer-token:}")
    private String bearerToken;

    @Value("${twitter.search.max-results:100}")
    private int maxResults;

    @Value("${twitter.search.include-retweets:false}")
    private boolean includeRetweets;

    private TwitterClient twitterClient;

    private static final Map<String, String> SYMBOL_HASHTAGS = Map.of(
            "BTC", "#Bitcoin OR $BTC",
            "ETH", "#Ethereum OR $ETH",
            "SOL", "#Solana OR $SOL",
            "DOGE", "#Dogecoin OR $DOGE",
            "XRP", "#XRP OR $XRP",
            "ADA", "#Cardano OR $ADA",
            "AVAX", "#Avalanche OR $AVAX",
            "DOT", "#Polkadot OR $DOT",
            "MATIC", "#Polygon OR $MATIC",
            "LINK", "#Chainlink OR $LINK"
    );

    @PostConstruct
    public void init() {
        if (bearerToken != null && !bearerToken.isBlank()) {
            twitterClient = new TwitterClient(TwitterCredentials.builder()
                    .bearerToken(bearerToken)
                    .build());
            log.info("Twitter client initialized successfully");
        } else {
            log.warn("Twitter bearer token not configured. Sentiment analysis will be unavailable.");
        }
    }

    public List<String> getTweetTexts(String symbol) {
        if (twitterClient == null) {
            log.warn("Twitter client not initialized. Cannot fetch tweets for {}", symbol);
            return Collections.emptyList();
        }

        String normalizedSymbol = extractBaseSymbol(symbol);
        String query = buildSearchQuery(normalizedSymbol);

        try {
            TweetList tweetList = twitterClient.searchTweets(query,
                    AdditionalParameters.builder()
                            .maxResults(maxResults)
                            .build());

            if (tweetList == null || tweetList.getData() == null) {
                log.info("No tweets found for query: {}", query);
                return Collections.emptyList();
            }

            List<String> texts = tweetList.getData().stream()
                    .map(TweetV2.TweetData::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .map(this::cleanTweetText)
                    .collect(Collectors.toList());

            log.info("Retrieved {} tweets for symbol {}", texts.size(), normalizedSymbol);
            return texts;
        } catch (Exception e) {
            log.error("Failed to fetch tweets for {}: {}", normalizedSymbol, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<TweetData> searchTweets(String symbol) {
        if (twitterClient == null) {
            return Collections.emptyList();
        }

        String normalizedSymbol = extractBaseSymbol(symbol);
        String query = buildSearchQuery(normalizedSymbol);

        try {
            TweetList tweetList = twitterClient.searchTweets(query,
                    AdditionalParameters.builder()
                            .maxResults(maxResults)
                            .build());

            if (tweetList == null || tweetList.getData() == null) {
                return Collections.emptyList();
            }

            return tweetList.getData().stream()
                    .map(tweet -> TweetData.builder()
                            .id(tweet.getId())
                            .text(cleanTweetText(tweet.getText()))
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to search tweets for {}: {}", normalizedSymbol, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String buildSearchQuery(String symbol) {
        String baseQuery = SYMBOL_HASHTAGS.getOrDefault(symbol.toUpperCase(), "$" + symbol.toUpperCase());

        StringBuilder query = new StringBuilder(baseQuery);
        query.append(" lang:en");

        if (!includeRetweets) {
            query.append(" -is:retweet");
        }

        return query.toString();
    }

    private String cleanTweetText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("https?://\\S+", "")
                .replaceAll("@\\w+", "")
                .replaceAll("#", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractBaseSymbol(String symbol) {
        if (symbol.contains("-")) {
            return symbol.split("-")[0].toUpperCase();
        }
        return symbol.toUpperCase();
    }

    public boolean isAvailable() {
        return twitterClient != null;
    }

    @lombok.Data
    @lombok.Builder
    public static class TweetData {
        private String id;
        private String text;
    }
}
