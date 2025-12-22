package com.tracker.controller;

import com.tracker.model.PriceHistory;
import com.tracker.model.PriceResponse;
import com.tracker.model.TrackingStatus;
import com.tracker.service.CoinbaseService;
import com.tracker.service.PriceTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PriceController {

    private final CoinbaseService coinbaseService;
    private final PriceTrackingService trackingService;

    @GetMapping("/price/{symbol}/spot")
    public ResponseEntity<PriceResponse> getSpotPrice(@PathVariable String symbol) {
        BigDecimal price = coinbaseService.getSpotPrice(symbol);
        String[] parts = parseSymbol(symbol);

        return ResponseEntity.ok(PriceResponse.builder()
                .symbol(parts[0])
                .currency(parts[1])
                .spotPrice(price)
                .timestamp(Instant.now())
                .build());
    }

    @GetMapping("/price/{symbol}/buy")
    public ResponseEntity<PriceResponse> getBuyPrice(@PathVariable String symbol) {
        BigDecimal price = coinbaseService.getBuyPrice(symbol);
        String[] parts = parseSymbol(symbol);

        return ResponseEntity.ok(PriceResponse.builder()
                .symbol(parts[0])
                .currency(parts[1])
                .buyPrice(price)
                .timestamp(Instant.now())
                .build());
    }

    @GetMapping("/price/{symbol}/sell")
    public ResponseEntity<PriceResponse> getSellPrice(@PathVariable String symbol) {
        BigDecimal price = coinbaseService.getSellPrice(symbol);
        String[] parts = parseSymbol(symbol);

        return ResponseEntity.ok(PriceResponse.builder()
                .symbol(parts[0])
                .currency(parts[1])
                .sellPrice(price)
                .timestamp(Instant.now())
                .build());
    }

    @GetMapping("/price/{symbol}/all")
    public ResponseEntity<PriceResponse> getAllPrices(@PathVariable String symbol) {
        PriceResponse prices = trackingService.recordAndGetPrice(symbol);
        return ResponseEntity.ok(prices);
    }

    @GetMapping("/price/{symbol}/history")
    public ResponseEntity<PriceHistory> getPriceHistory(@PathVariable String symbol) {
        PriceHistory history = trackingService.getPriceHistory(symbol);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/track/{symbol}")
    public ResponseEntity<Map<String, Object>> startTracking(@PathVariable String symbol) {
        trackingService.startTracking(symbol);
        return ResponseEntity.ok(Map.of(
                "message", "Started tracking " + symbol.toUpperCase(),
                "symbol", symbol.toUpperCase(),
                "timestamp", Instant.now()
        ));
    }

    @DeleteMapping("/track/{symbol}")
    public ResponseEntity<Map<String, Object>> stopTracking(@PathVariable String symbol) {
        trackingService.stopTracking(symbol);
        return ResponseEntity.ok(Map.of(
                "message", "Stopped tracking " + symbol.toUpperCase(),
                "symbol", symbol.toUpperCase(),
                "timestamp", Instant.now()
        ));
    }

    @GetMapping("/track")
    public ResponseEntity<TrackingStatus> getTrackedSymbols() {
        return ResponseEntity.ok(trackingService.getTrackingStatus());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "coinbase-price-tracker",
                "timestamp", Instant.now()
        ));
    }

    private String[] parseSymbol(String symbol) {
        String normalized = symbol.contains("-") ? symbol.toUpperCase() : symbol.toUpperCase() + "-USD";
        String[] parts = normalized.split("-");
        return new String[]{parts[0], parts.length > 1 ? parts[1] : "USD"};
    }
}
