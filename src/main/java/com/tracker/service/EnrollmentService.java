package com.tracker.service;

import com.tracker.exception.SymbolNotEnrolledException;
import com.tracker.model.Enrollment;
import com.tracker.model.EnrollmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final DynamoDbTable<Enrollment> enrollmentTable;

    @Value("${recommendation.minimum-enrollment-days:7}")
    private int minimumEnrollmentDays;

    public EnrollmentResponse enrollSymbol(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Instant now = Instant.now();

        Enrollment existing = getEnrollment(normalizedSymbol);
        if (existing != null && Enrollment.Status.ACTIVE.name().equals(existing.getStatus())) {
            log.info("Symbol {} is already enrolled", normalizedSymbol);
            return buildEnrollmentResponse(existing);
        }

        Enrollment enrollment = Enrollment.builder()
                .symbol(normalizedSymbol)
                .enrolledAt(now)
                .status(Enrollment.Status.ACTIVE.name())
                .updatedAt(now)
                .build();

        enrollmentTable.putItem(enrollment);
        log.info("Enrolled symbol: {}", normalizedSymbol);

        return buildEnrollmentResponse(enrollment);
    }

    public EnrollmentResponse getEnrollmentStatus(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Enrollment enrollment = getEnrollment(normalizedSymbol);

        if (enrollment == null) {
            throw new SymbolNotEnrolledException(symbol);
        }

        return buildEnrollmentResponse(enrollment);
    }

    public boolean isEnrolled(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Enrollment enrollment = getEnrollment(normalizedSymbol);
        return enrollment != null && Enrollment.Status.ACTIVE.name().equals(enrollment.getStatus());
    }

    public boolean isRecommendationAvailable(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Enrollment enrollment = getEnrollment(normalizedSymbol);

        if (enrollment == null || !Enrollment.Status.ACTIVE.name().equals(enrollment.getStatus())) {
            return false;
        }

        Duration enrollmentDuration = Duration.between(enrollment.getEnrolledAt(), Instant.now());
        return enrollmentDuration.toDays() >= minimumEnrollmentDays;
    }

    public Instant getRecommendationAvailableDate(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Enrollment enrollment = getEnrollment(normalizedSymbol);

        if (enrollment == null) {
            throw new SymbolNotEnrolledException(symbol);
        }

        return enrollment.getEnrolledAt().plus(Duration.ofDays(minimumEnrollmentDays));
    }

    public long getDaysUntilRecommendation(String symbol) {
        Instant availableAt = getRecommendationAvailableDate(symbol);
        Duration remaining = Duration.between(Instant.now(), availableAt);
        return Math.max(0, remaining.toDays());
    }

    public void unenrollSymbol(String symbol) {
        String normalizedSymbol = normalizeSymbol(symbol);
        Enrollment enrollment = getEnrollment(normalizedSymbol);

        if (enrollment == null) {
            throw new SymbolNotEnrolledException(symbol);
        }

        enrollment.setStatus(Enrollment.Status.INACTIVE.name());
        enrollment.setUpdatedAt(Instant.now());
        enrollmentTable.putItem(enrollment);

        log.info("Unenrolled symbol: {}", normalizedSymbol);
    }

    public List<EnrollmentResponse> getAllEnrollments() {
        List<EnrollmentResponse> enrollments = new ArrayList<>();

        enrollmentTable.scan().items().forEach(enrollment -> {
            if (Enrollment.Status.ACTIVE.name().equals(enrollment.getStatus())) {
                enrollments.add(buildEnrollmentResponse(enrollment));
            }
        });

        return enrollments;
    }

    private Enrollment getEnrollment(String normalizedSymbol) {
        Key key = Key.builder()
                .partitionValue(normalizedSymbol)
                .build();
        return enrollmentTable.getItem(key);
    }

    private EnrollmentResponse buildEnrollmentResponse(Enrollment enrollment) {
        Instant availableAt = enrollment.getEnrolledAt().plus(Duration.ofDays(minimumEnrollmentDays));
        Duration remaining = Duration.between(Instant.now(), availableAt);
        long daysRemaining = Math.max(0, remaining.toDays());
        boolean isAvailable = daysRemaining == 0;

        String[] parts = enrollment.getSymbol().split("-");
        String baseSymbol = parts[0];
        String currency = parts.length > 1 ? parts[1] : "USD";

        return EnrollmentResponse.builder()
                .symbol(baseSymbol)
                .currency(currency)
                .enrolledAt(enrollment.getEnrolledAt())
                .status(enrollment.getStatus())
                .recommendationAvailable(isAvailable)
                .recommendationAvailableAt(availableAt)
                .daysUntilRecommendation(daysRemaining)
                .build();
    }

    private String normalizeSymbol(String symbol) {
        if (symbol.contains("-")) {
            return symbol.toUpperCase();
        }
        return symbol.toUpperCase() + "-USD";
    }
}
