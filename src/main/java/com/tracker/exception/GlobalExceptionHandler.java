package com.tracker.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientException(WebClientResponseException ex) {
        log.error("Coinbase API error: {} - {}", ex.getStatusCode(), ex.getMessage());

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = switch (status) {
            case NOT_FOUND -> "Symbol not found or not supported by Coinbase";
            case TOO_MANY_REQUESTS -> "Rate limit exceeded. Please try again later";
            case SERVICE_UNAVAILABLE -> "Coinbase API is temporarily unavailable";
            default -> "Error communicating with Coinbase API";
        };

        return ResponseEntity.status(status).body(Map.of(
                "error", message,
                "status", status.value(),
                "timestamp", Instant.now()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());

        return ResponseEntity.badRequest().body(Map.of(
                "error", ex.getMessage(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "timestamp", Instant.now()
        ));
    }

    @ExceptionHandler(SymbolNotEnrolledException.class)
    public ResponseEntity<Map<String, Object>> handleSymbolNotEnrolled(SymbolNotEnrolledException ex) {
        log.warn("Symbol not enrolled: {}", ex.getSymbol());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", ex.getMessage(),
                "symbol", ex.getSymbol(),
                "action", "Enroll using POST /api/v1/enroll/" + ex.getSymbol(),
                "status", HttpStatus.NOT_FOUND.value(),
                "timestamp", Instant.now()
        ));
    }

    @ExceptionHandler(RecommendationNotAvailableException.class)
    public ResponseEntity<Map<String, Object>> handleRecommendationNotAvailable(RecommendationNotAvailableException ex) {
        log.info("Recommendation not yet available for {}: {} days remaining", ex.getSymbol(), ex.getDaysRemaining());

        return ResponseEntity.badRequest().body(Map.of(
                "error", "Recommendation not yet available",
                "symbol", ex.getSymbol(),
                "availableAt", ex.getAvailableAt().toString(),
                "daysRemaining", ex.getDaysRemaining(),
                "status", HttpStatus.BAD_REQUEST.value(),
                "timestamp", Instant.now()
        ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "An unexpected error occurred",
                "message", ex.getMessage(),
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "timestamp", Instant.now()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Internal server error",
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "timestamp", Instant.now()
        ));
    }
}
