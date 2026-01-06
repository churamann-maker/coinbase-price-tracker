package com.tracker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@Configuration
public class CognitoConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${cognito.user-pool-id:}")
    private String userPoolId;

    @Value("${cognito.client-id:}")
    private String clientId;

    @Bean
    public CognitoIdentityProviderClient cognitoClient() {
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(awsRegion))
                .build();
    }

    @Bean
    public String cognitoUserPoolId() {
        return userPoolId;
    }

    @Bean
    public String cognitoClientId() {
        return clientId;
    }
}
