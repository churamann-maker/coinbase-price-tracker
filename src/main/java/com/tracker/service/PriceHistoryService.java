package com.tracker.service;

import com.tracker.model.PriceRecord;
import com.tracker.model.PriceResponse;
import com.tracker.model.TrendData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceHistoryService {

    private final DynamoDbTable<PriceRecord> priceRecordTable;
    private final CoinbaseService coinbaseService;

    @Value("${price-history.retention-days:30}")
    private int retentionDays;

    @Value("${recommendation.trend.moving-average-days:7}")
    private int movingAverageDays;

    public void recordPrice(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);

        try {
            PriceResponse prices = coinbaseService.getAllPrices(normalizedSymbol);
            Instant now = Instant.now();

            PriceRecord record = PriceRecord.builder()
                    .symbol(normalizedSymbol)
                    .timestamp(now)
                    .spotPrice(prices.getSpotPrice())
                    .buyPrice(prices.getBuyPrice())
                    .sellPrice(prices.getSellPrice())
                    .ttl(now.plus(Duration.ofDays(retentionDays)).getEpochSecond())
                    .build();

            priceRecordTable.putItem(record);
            log.debug("Recorded price for {}: sell={}", normalizedSymbol, prices.getSellPrice());
        } catch (Exception e) {
            log.error("Failed to record price for {}: {}", normalizedSymbol, e.getMessage());
        }
    }

    public List<PriceRecord> getPriceHistory(String symbol, int days) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Instant startTime = Instant.now().minus(Duration.ofDays(days));

        QueryConditional queryConditional = QueryConditional.sortGreaterThanOrEqualTo(
                Key.builder()
                        .partitionValue(normalizedSymbol)
                        .sortValue(startTime.toString())
                        .build()
        );

        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .build();

        List<PriceRecord> records = new ArrayList<>();
        priceRecordTable.query(request).items().forEach(records::add);

        return records;
    }

    public BigDecimal calculateMovingAverage(String symbol, int days) {
        List<PriceRecord> records = getPriceHistory(symbol, days);

        if (records.isEmpty()) {
            log.warn("No price history available for {} to calculate moving average", symbol);
            return null;
        }

        BigDecimal sum = records.stream()
                .map(PriceRecord::getSellPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = records.stream()
                .filter(record -> record.getSellPrice() != null)
                .count();

        if (count == 0) {
            return null;
        }

        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    public TrendData analyzeTrend(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);

        BigDecimal movingAverage = calculateMovingAverage(normalizedSymbol, movingAverageDays);
        PriceResponse currentPrices = coinbaseService.getAllPrices(normalizedSymbol);
        BigDecimal currentPrice = currentPrices.getSellPrice();

        if (movingAverage == null || currentPrice == null) {
            log.warn("Unable to analyze trend for {}: insufficient data", normalizedSymbol);
            return TrendData.builder()
                    .currentPrice(currentPrice)
                    .sevenDayMovingAverage(movingAverage)
                    .trendingUpwards(false)
                    .percentAboveAverage(0.0)
                    .build();
        }

        boolean trendingUp = currentPrice.compareTo(movingAverage) > 0;
        double percentAbove = currentPrice.subtract(movingAverage)
                .divide(movingAverage, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        return TrendData.builder()
                .currentPrice(currentPrice)
                .sevenDayMovingAverage(movingAverage)
                .trendingUpwards(trendingUp)
                .percentAboveAverage(percentAbove)
                .build();
    }

    public boolean isTrendingUpwards(String symbol) {
        TrendData trendData = analyzeTrend(symbol);
        return trendData.isTrendingUpwards();
    }

    private String normalizeSymbol(String symbol) {
        if (symbol.contains("-")) {
            return symbol.toUpperCase();
        }
        return symbol.toUpperCase() + "-USD";
    }
}
