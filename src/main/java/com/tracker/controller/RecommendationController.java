package com.tracker.controller;

import com.tracker.model.EnrollmentResponse;
import com.tracker.model.RecommendationResponse;
import com.tracker.service.EnrollmentService;
import com.tracker.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final EnrollmentService enrollmentService;

    @PostMapping("/enroll/{symbol}")
    public ResponseEntity<EnrollmentResponse> enrollSymbol(@PathVariable String symbol) {
        EnrollmentResponse response = enrollmentService.enrollSymbol(symbol);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/enroll/{symbol}")
    public ResponseEntity<EnrollmentResponse> getEnrollmentStatus(@PathVariable String symbol) {
        EnrollmentResponse response = enrollmentService.getEnrollmentStatus(symbol);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/enroll/{symbol}")
    public ResponseEntity<Map<String, Object>> unenrollSymbol(@PathVariable String symbol) {
        enrollmentService.unenrollSymbol(symbol);
        return ResponseEntity.ok(Map.of(
                "message", "Successfully unenrolled " + symbol.toUpperCase(),
                "symbol", symbol.toUpperCase(),
                "timestamp", Instant.now()
        ));
    }

    @GetMapping("/enroll")
    public ResponseEntity<List<EnrollmentResponse>> listEnrollments() {
        List<EnrollmentResponse> enrollments = enrollmentService.getAllEnrollments();
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/recommendation/{symbol}")
    public ResponseEntity<RecommendationResponse> getRecommendation(@PathVariable String symbol) {
        RecommendationResponse response = recommendationService.getRecommendation(symbol);
        return ResponseEntity.ok(response);
    }
}
