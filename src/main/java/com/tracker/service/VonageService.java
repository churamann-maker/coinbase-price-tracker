package com.tracker.service;

import com.tracker.config.VonageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VonageService {

    private final VonageConfig vonageConfig;
    private final WebClient webClient;

    public boolean sendSms(String toPhoneNumber, String message) {
        log.info("Sending SMS to: {}", maskPhoneNumber(toPhoneNumber));

        try {
            Map<String, String> requestBody = Map.of(
                    "api_key", vonageConfig.getApiKey(),
                    "api_secret", vonageConfig.getApiSecret(),
                    "from", vonageConfig.getFromNumber(),
                    "to", toPhoneNumber.replace("+", ""),
                    "text", message
            );

            String response = webClient.post()
                    .uri(vonageConfig.getApiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("SMS sent successfully to: {}", maskPhoneNumber(toPhoneNumber));
            log.debug("Vonage response: {}", response);

            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", maskPhoneNumber(toPhoneNumber), e.getMessage());
            return false;
        }
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
    }
}
