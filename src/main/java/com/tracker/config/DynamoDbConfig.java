package com.tracker.config;

import com.tracker.model.Enrollment;
import com.tracker.model.PriceRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.dynamodb.endpoint:}")
    private String dynamoDbEndpoint;

    @Value("${aws.dynamodb.tables.enrollment:symbol-enrollments}")
    private String enrollmentTableName;

    @Value("${aws.dynamodb.tables.price-history:price-history}")
    private String priceHistoryTableName;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
                .region(Region.of(awsRegion));

        if (dynamoDbEndpoint != null && !dynamoDbEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(dynamoDbEndpoint));
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public DynamoDbTable<Enrollment> enrollmentTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(enrollmentTableName, TableSchema.fromBean(Enrollment.class));
    }

    @Bean
    public DynamoDbTable<PriceRecord> priceRecordTable(DynamoDbEnhancedClient enhancedClient) {
        return enhancedClient.table(priceHistoryTableName, TableSchema.fromBean(PriceRecord.class));
    }
}
