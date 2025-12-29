package com.tracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisResponse {

    private String sentiment;
    private String emoji;

    @JsonProperty("analysis_reasoning")
    private String analysisReasoning;

    public boolean isBullish() {
        return "bullish".equalsIgnoreCase(sentiment);
    }

    public boolean isBearish() {
        return "bearish".equalsIgnoreCase(sentiment);
    }
}
