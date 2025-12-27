package com.tracker.controller;

import com.tracker.model.SubscriptionRequest;
import com.tracker.model.SubscriptionResponse;
import com.tracker.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/subscribe")
    public ResponseEntity<SubscriptionResponse> subscribe(
            @Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse response = subscriptionService.subscribe(request.getPhoneNumber(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
