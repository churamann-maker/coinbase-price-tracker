package com.tracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.comprehend.ComprehendClient;

@Configuration
public class AwsComprehendConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public ComprehendClient comprehendClient() {
        return ComprehendClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }
}
