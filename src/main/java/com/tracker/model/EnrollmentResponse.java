package com.tracker.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class EnrollmentResponse {
    private String symbol;
    private String currency;
    private Instant enrolledAt;
    private String status;
    private boolean recommendationAvailable;
    private Instant recommendationAvailableAt;
    private long daysUntilRecommendation;
}
