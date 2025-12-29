package com.tracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
public class AnalysisConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${analysis.lambda.function-name:crypto-analysis-dev}")
    private String analysisFunctionName;

    @Bean
    public LambdaClient lambdaClient() {
        return LambdaClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    @Bean
    public String analysisFunctionName() {
        return analysisFunctionName;
    }
}
