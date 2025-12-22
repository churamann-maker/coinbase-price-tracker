package com.tracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class PriceRecord {

    private String symbol;
    private Instant timestamp;
    private BigDecimal spotPrice;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private Long ttl;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("symbol")
    public String getSymbol() {
        return symbol;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("timestamp")
    public Instant getTimestamp() {
        return timestamp;
    }
}
