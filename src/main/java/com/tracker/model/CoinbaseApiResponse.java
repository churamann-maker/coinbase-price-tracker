package com.tracker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CoinbaseApiResponse {
    private PriceData data;

    @Data
    public static class PriceData {
        private String base;
        private String currency;
        private String amount;

        @JsonProperty("base")
        public String getBase() {
            return base;
        }

        @JsonProperty("currency")
        public String getCurrency() {
            return currency;
        }

        @JsonProperty("amount")
        public String getAmount() {
            return amount;
        }
    }
}
