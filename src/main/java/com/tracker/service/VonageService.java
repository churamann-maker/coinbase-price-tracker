package com.tracker.service;

import com.tracker.config.VonageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VonageService {

    private final VonageConfig vonageConfig;
    private final WebClient webClient;

    public boolean sendSms(String toPhoneNumber, String message) {
        log.info("Sending WhatsApp message to: {}", maskPhoneNumber(toPhoneNumber));

        try {
            // Build Basic Auth header
            String credentials = vonageConfig.getApiKey() + ":" + vonageConfig.getApiSecret();
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            // Build WhatsApp message request body
            Map<String, String> requestBody = Map.of(
                    "from", vonageConfig.getFromNumber(),
                    "to", toPhoneNumber.replace("+", ""),
                    "message_type", "text",
                    "text", message,
                    "channel", "whatsapp"
            );

            String response = webClient.post()
                    .uri(vonageConfig.getApiUrl())
                    .header("Authorization", "Basic " + encodedCredentials)
                    .header("Accept", "application/json")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("WhatsApp message sent successfully to: {}", maskPhoneNumber(toPhoneNumber));
            log.debug("Vonage response: {}", response);

            return true;
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message to {}: {}", maskPhoneNumber(toPhoneNumber), e.getMessage());
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
