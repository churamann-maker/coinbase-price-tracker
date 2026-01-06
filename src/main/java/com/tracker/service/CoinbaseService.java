package com.tracker.service;

import com.tracker.model.CoinbaseApiResponse;
import com.tracker.model.CoinInfo;
import com.tracker.model.PriceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CoinbaseService {

    private final WebClient webClient;

    public CoinbaseService(@Value("${coinbase.api.base-url:https://api.coinbase.com/v2}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    public BigDecimal getSpotPrice(String symbol) {
        return fetchPrice(symbol, "spot");
    }

    public BigDecimal getBuyPrice(String symbol) {
        return fetchPrice(symbol, "buy");
    }

    public BigDecimal getSellPrice(String symbol) {
        return fetchPrice(symbol, "sell");
    }

    public PriceResponse getAllPrices(String symbol) {
        String[] parts = parseSymbol(symbol);
        String base = parts[0];
        String currency = parts[1];

        BigDecimal spotPrice = getSpotPrice(symbol);
        BigDecimal buyPrice = getBuyPrice(symbol);
        BigDecimal sellPrice = getSellPrice(symbol);

        return PriceResponse.builder()
                .symbol(base)
                .currency(currency)
                .spotPrice(spotPrice)
                .buyPrice(buyPrice)
                .sellPrice(sellPrice)
                .timestamp(Instant.now())
                .build();
    }

    private BigDecimal fetchPrice(String symbol, String priceType) {
        try {
            String normalizedSymbol = normalizeSymbol(symbol);
            String path = String.format("/prices/%s/%s", normalizedSymbol, priceType);

            log.debug("Fetching {} price for symbol: {}", priceType, normalizedSymbol);

            CoinbaseApiResponse response = webClient.get()
                    .uri(path)
                    .retrieve()
                    .bodyToMono(CoinbaseApiResponse.class)
                    .block();

            if (response != null && response.getData() != null) {
                return new BigDecimal(response.getData().getAmount());
            }

            throw new RuntimeException("Empty response from Coinbase API");
        } catch (WebClientResponseException e) {
            log.error("Coinbase API error for {} {}: {} - {}",
                    priceType, symbol, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch " + priceType + " price: " + e.getMessage());
        }
    }

    private String normalizeSymbol(String symbol) {
        if (symbol.contains("-")) {
            return symbol.toUpperCase();
        }
        return symbol.toUpperCase() + "-USD";
    }

    private String[] parseSymbol(String symbol) {
        String normalized = normalizeSymbol(symbol);
        String[] parts = normalized.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid symbol format. Expected format: BASE-CURRENCY (e.g., BTC-USD)");
        }
        return parts;
    }

    @SuppressWarnings("unchecked")
    public List<CoinInfo> getPopularCoins(int limit) {
        try {
            log.info("Fetching popular crypto coins from Coinbase, limit: {}", limit);

            Map<String, Object> response = webClient.get()
                    .uri("/currencies/crypto")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            List<CoinInfo> coins = new ArrayList<>();

            if (response != null && response.get("data") != null) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");

                for (Map<String, Object> currency : data) {
                    String code = (String) currency.get("code");
                    String name = (String) currency.get("name");

                    if (code != null && name != null) {
                        coins.add(CoinInfo.builder()
                                .symbol(code)
                                .name(name)
                                .displayName(name + " (" + code + ")")
                                .build());
                    }

                    if (coins.size() >= limit) {
                        break;
                    }
                }
            }

            log.info("Retrieved {} crypto coins from Coinbase", coins.size());
            return coins;

        } catch (WebClientResponseException e) {
            log.error("Coinbase API error fetching crypto currencies: {} - {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch currencies: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error fetching crypto currencies: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch currencies: " + e.getMessage());
        }
    }
}
