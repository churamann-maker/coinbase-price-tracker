package com.tracker.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class VonageConfig {

    @Value("${vonage.api.key:}")
    private String apiKey;

    @Value("${vonage.api.secret:}")
    private String apiSecret;

    @Value("${vonage.from.number:14157386102}")
    private String fromNumber;

    @Value("${vonage.api.url:https://messages-sandbox.nexmo.com/v1/messages}")
    private String apiUrl;
}
