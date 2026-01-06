package com.tracker.function;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tracker.model.*;
import com.tracker.service.CognitoService;
import com.tracker.service.CoinbaseService;
import com.tracker.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ApiGatewayFunctions {

    private final SubscriptionService subscriptionService;
    private final CoinbaseService coinbaseService;
    private final CognitoService cognitoService;
    private final ObjectMapper objectMapper;

    @Bean
    public Function<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> apiHandler() {
        return request -> {
            String path = request.getPath();
            String method = request.getHttpMethod();

            log.info("Received request: {} {}", method, path);

            try {
                // Health check
                if (path.endsWith("/health") && "GET".equals(method)) {
                    return healthCheck();
                }

                // Subscribe endpoint
                if (path.endsWith("/subscribe") && "POST".equals(method)) {
                    return handleSubscribe(request);
                }

                // Auth endpoints
                if (path.endsWith("/auth/signup") && "POST".equals(method)) {
                    return handleSignUp(request);
                }

                if (path.endsWith("/auth/signin") && "POST".equals(method)) {
                    return handleSignIn(request);
                }

                if (path.endsWith("/auth/complete-signup") && "POST".equals(method)) {
                    return handleCompleteSignup(request);
                }

                if (path.endsWith("/coins/popular") && "GET".equals(method)) {
                    return handleGetPopularCoins(request);
                }

                if (path.endsWith("/auth/coins") && "PUT".equals(method)) {
                    return handleUpdateCoins(request);
                }

                // Get price endpoint
                if (path.matches(".*/prices/[A-Z]+") && "GET".equals(method)) {
                    String symbol = path.substring(path.lastIndexOf('/') + 1);
                    return handleGetPrice(symbol);
                }

                // Not found
                return createResponse(404, Map.of(
                        "error", "Not Found",
                        "path", path,
                        "method", method
                ));

            } catch (Exception e) {
                log.error("Error processing request: {}", e.getMessage(), e);
                return createResponse(500, Map.of(
                        "error", "Internal Server Error",
                        "message", e.getMessage()
                ));
            }
        };
    }

    private APIGatewayProxyResponseEvent healthCheck() {
        return createResponse(200, Map.of(
                "status", "UP",
                "service", "coinbase-price-tracker",
                "timestamp", Instant.now().toString()
        ));
    }

    private APIGatewayProxyResponseEvent handleSubscribe(APIGatewayProxyRequestEvent request) {
        try {
            String body = request.getBody();
            log.info("Subscribe request body: {}", body);

            SubscriptionRequest subRequest = objectMapper.readValue(body, SubscriptionRequest.class);
            SubscriptionResponse response = subscriptionService.subscribe(subRequest.getPhoneNumber(), subRequest.getName());

            return createResponse(201, response);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse subscription request: {}", e.getMessage());
            return createResponse(400, Map.of(
                    "error", "Invalid request body",
                    "message", e.getMessage()
            ));
        }
    }

    private APIGatewayProxyResponseEvent handleGetPrice(String symbol) {
        try {
            var price = coinbaseService.getAllPrices(symbol);
            return createResponse(200, price);
        } catch (Exception e) {
            log.error("Failed to get price for {}: {}", symbol, e.getMessage());
            return createResponse(500, Map.of(
                    "error", "Failed to get price",
                    "symbol", symbol,
                    "message", e.getMessage()
            ));
        }
    }

    private APIGatewayProxyResponseEvent handleSignUp(APIGatewayProxyRequestEvent request) {
        try {
            String body = request.getBody();
            log.info("SignUp request received");

            AuthRequest authRequest = objectMapper.readValue(body, AuthRequest.class);
            AuthResponse response = cognitoService.signUp(authRequest);

            return createResponse(response.isSuccess() ? 201 : 400, response);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse signup request: {}", e.getMessage());
            return createResponse(400, Map.of(
                    "success", false,
                    "message", "Invalid request body"
            ));
        }
    }

    private APIGatewayProxyResponseEvent handleSignIn(APIGatewayProxyRequestEvent request) {
        try {
            String body = request.getBody();
            log.info("SignIn request received");

            AuthRequest authRequest = objectMapper.readValue(body, AuthRequest.class);
            AuthResponse response = cognitoService.signIn(authRequest);

            if (response.isSuccess()) {
                Subscriber subscriber = subscriptionService.getSubscriberByPhoneNumber(authRequest.getPhoneNumber());
                response.setSubscriber(subscriber);
            }

            return createResponse(response.isSuccess() ? 200 : 401, response);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse signin request: {}", e.getMessage());
            return createResponse(400, Map.of(
                    "success", false,
                    "message", "Invalid request body"
            ));
        }
    }

    private APIGatewayProxyResponseEvent handleCompleteSignup(APIGatewayProxyRequestEvent request) {
        try {
            String body = request.getBody();
            log.info("CompleteSignup request received");

            AuthRequest authRequest = objectMapper.readValue(body, AuthRequest.class);

            // Ensure BTC is included
            List<String> coins = authRequest.getSelectedCoins();
            if (coins != null && !coins.contains("BTC")) {
                coins.add(0, "BTC");
            }

            SubscriptionResponse response = subscriptionService.subscribeWithCoins(
                    authRequest.getPhoneNumber(),
                    authRequest.getName(),
                    authRequest.getVerificationCode(),
                    coins
            );

            return createResponse(201, response);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse complete-signup request: {}", e.getMessage());
            return createResponse(400, Map.of(
                    "error", "Invalid request body",
                    "message", e.getMessage()
            ));
        }
    }

    private APIGatewayProxyResponseEvent handleGetPopularCoins(APIGatewayProxyRequestEvent request) {
        try {
            Map<String, String> queryParams = request.getQueryStringParameters();
            int limit = 100;
            if (queryParams != null && queryParams.containsKey("limit")) {
                limit = Integer.parseInt(queryParams.get("limit"));
            }

            List<CoinInfo> coins = coinbaseService.getPopularCoins(limit);
            return createResponse(200, coins);
        } catch (Exception e) {
            log.error("Failed to get popular coins: {}", e.getMessage());
            return createResponse(500, Map.of(
                    "error", "Failed to get coins",
                    "message", e.getMessage()
            ));
        }
    }

    private APIGatewayProxyResponseEvent handleUpdateCoins(APIGatewayProxyRequestEvent request) {
        try {
            String body = request.getBody();
            log.info("UpdateCoins request received");

            AuthRequest authRequest = objectMapper.readValue(body, AuthRequest.class);

            // Ensure BTC is included
            List<String> coins = authRequest.getSelectedCoins();
            if (coins != null && !coins.contains("BTC")) {
                coins.add(0, "BTC");
            }

            boolean updated = subscriptionService.updateSelectedCoins(authRequest.getPhoneNumber(), coins);

            if (updated) {
                Subscriber subscriber = subscriptionService.getSubscriberByPhoneNumber(authRequest.getPhoneNumber());
                return createResponse(200, AuthResponse.builder()
                        .success(true)
                        .message("Coins updated successfully")
                        .subscriber(subscriber)
                        .build());
            }

            return createResponse(404, AuthResponse.builder()
                    .success(false)
                    .message("Subscriber not found")
                    .build());
        } catch (JsonProcessingException e) {
            log.error("Failed to parse update-coins request: {}", e.getMessage());
            return createResponse(400, Map.of(
                    "success", false,
                    "message", "Invalid request body"
            ));
        }
    }

    private APIGatewayProxyResponseEvent createResponse(int statusCode, Object body) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(Map.of(
                "Content-Type", "application/json",
                "Access-Control-Allow-Origin", "*",
                "Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS",
                "Access-Control-Allow-Headers", "Content-Type, Authorization"
        ));

        try {
            response.setBody(objectMapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response: {}", e.getMessage());
            response.setBody("{\"error\":\"Failed to serialize response\"}");
        }

        return response;
    }
}
