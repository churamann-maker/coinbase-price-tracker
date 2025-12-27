package com.tracker.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SubscriptionResponse {
    private String phoneNumber;
    private String message;
    private Instant subscribedAt;
}
