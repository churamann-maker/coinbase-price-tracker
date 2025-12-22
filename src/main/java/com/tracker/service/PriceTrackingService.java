package com.tracker.service;

import com.tracker.model.PriceHistory;
import com.tracker.model.PriceResponse;
import com.tracker.model.TrackingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceTrackingService {

    private final CoinbaseService coinbaseService;

    private final Set<String> trackedSymbols = ConcurrentHashMap.newKeySet();
    private final Map<String, List<PriceHistory.PricePoint>> priceHistoryMap = new ConcurrentHashMap<>();

    @Value("${tracker.history.max-records:100}")
    private int maxHistoryRecords;

    public void startTracking(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        trackedSymbols.add(normalizedSymbol);
        priceHistoryMap.putIfAbsent(normalizedSymbol, Collections.synchronizedList(new ArrayList<>()));
        log.info("Started tracking symbol: {}", normalizedSymbol);

        recordCurrentPrice(normalizedSymbol);
    }

    public void stopTracking(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        trackedSymbols.remove(normalizedSymbol);
        priceHistoryMap.remove(normalizedSymbol);
        log.info("Stopped tracking symbol: {}", normalizedSymbol);
    }

    public TrackingStatus getTrackingStatus() {
        return TrackingStatus.builder()
                .trackedSymbols(new HashSet<>(trackedSymbols))
                .totalTracked(trackedSymbols.size())
                .timestamp(Instant.now())
                .build();
    }

    public boolean isTracking(String symbol) {
        return trackedSymbols.contains(normalizeSymbol(symbol));
    }

    public PriceHistory getPriceHistory(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);

        if (!isTracking(symbol)) {
            startTracking(symbol);
        }

        recordCurrentPrice(normalizedSymbol);

        List<PriceHistory.PricePoint> history = priceHistoryMap.getOrDefault(
                normalizedSymbol, Collections.emptyList());

        String[] parts = normalizedSymbol.split("-");
        return PriceHistory.builder()
                .symbol(parts[0])
                .currency(parts.length > 1 ? parts[1] : "USD")
                .history(new ArrayList<>(history))
                .totalRecords(history.size())
                .build();
    }

    public PriceResponse recordAndGetPrice(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);

        if (isTracking(symbol)) {
            recordCurrentPrice(normalizedSymbol);
        }

        return coinbaseService.getAllPrices(normalizedSymbol);
    }

    private void recordCurrentPrice(String normalizedSymbol) {
        try {
            PriceResponse prices = coinbaseService.getAllPrices(normalizedSymbol);

            PriceHistory.PricePoint point = PriceHistory.PricePoint.builder()
                    .spotPrice(prices.getSpotPrice())
                    .buyPrice(prices.getBuyPrice())
                    .sellPrice(prices.getSellPrice())
                    .timestamp(Instant.now())
                    .build();

            List<PriceHistory.PricePoint> history = priceHistoryMap.computeIfAbsent(
                    normalizedSymbol, k -> Collections.synchronizedList(new ArrayList<>()));

            history.add(point);

            while (history.size() > maxHistoryRecords) {
                history.remove(0);
            }

            log.debug("Recorded price for {}: spot={}, buy={}, sell={}",
                    normalizedSymbol, prices.getSpotPrice(), prices.getBuyPrice(), prices.getSellPrice());
        } catch (Exception e) {
            log.error("Failed to record price for {}: {}", normalizedSymbol, e.getMessage());
        }
    }

    private String normalizeSymbol(String symbol) {
        if (symbol.contains("-")) {
            return symbol.toUpperCase();
        }
        return symbol.toUpperCase() + "-USD";
    }
}
