package com.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class Enrollment {

    private String symbol;
    private Instant enrolledAt;
    private String status;
    private Instant updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("symbol")
    public String getSymbol() {
        return symbol;
    }

    public enum Status {
        ACTIVE,
        INACTIVE
    }
}
