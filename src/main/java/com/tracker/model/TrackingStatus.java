package com.tracker.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
public class TrackingStatus {
    private Set<String> trackedSymbols;
    private int totalTracked;
    private Instant timestamp;
}
