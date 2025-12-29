package com.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.model.AnalysisResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@Slf4j
public class AnalysisClient {

    private static final String DEFAULT_SYMBOL = "BTC";
    private static final String DEFAULT_EMOJI = "";

    private final LambdaClient lambdaClient;
    private final String functionName;
    private final ObjectMapper objectMapper;

    public AnalysisClient(
            LambdaClient lambdaClient,
            @Qualifier("analysisFunctionName") String functionName,
            ObjectMapper objectMapper) {
        this.lambdaClient = lambdaClient;
        this.functionName = functionName;
        this.objectMapper = objectMapper;
    }

    public AnalysisResponse getMarketAnalysis() {
        return getMarketAnalysis(DEFAULT_SYMBOL);
    }

    public AnalysisResponse getMarketAnalysis(String symbol) {
        log.info("Invoking analysis Lambda for symbol: {}", symbol);

        try {
            String payload = objectMapper.writeValueAsString(Map.of("symbol", symbol));

            InvokeRequest request = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromString(payload, StandardCharsets.UTF_8))
                    .build();

            InvokeResponse response = lambdaClient.invoke(request);

            if (response.functionError() != null) {
                log.error("Analysis Lambda returned error: {}", response.functionError());
                return createDefaultResponse();
            }

            String responsePayload = response.payload().asUtf8String();
            log.debug("Analysis Lambda response: {}", responsePayload);

            // Parse the outer response (API Gateway format)
            Map<String, Object> outerResponse = objectMapper.readValue(responsePayload, Map.class);

            // The body is a JSON string that needs to be parsed
            String body = (String) outerResponse.get("body");
            if (body != null) {
                return objectMapper.readValue(body, AnalysisResponse.class);
            }

            log.warn("No body in analysis response");
            return createDefaultResponse();

        } catch (Exception e) {
            log.error("Error invoking analysis Lambda: {}", e.getMessage(), e);
            return createDefaultResponse();
        }
    }

    private AnalysisResponse createDefaultResponse() {
        return AnalysisResponse.builder()
                .sentiment("unknown")
                .emoji(DEFAULT_EMOJI)
                .analysisReasoning("Analysis unavailable")
                .build();
    }
}
