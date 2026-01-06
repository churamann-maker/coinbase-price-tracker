package com.tracker.controller;

import com.tracker.model.*;
import com.tracker.service.CognitoService;
import com.tracker.service.CoinbaseService;
import com.tracker.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final CognitoService cognitoService;
    private final SubscriptionService subscriptionService;
    private final CoinbaseService coinbaseService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody AuthRequest request) {
        log.info("Sign up request for phone: {}", maskPhone(request.getPhoneNumber()));

        AuthResponse response = cognitoService.signUp(request);

        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verifyPhone(@Valid @RequestBody AuthRequest request) {
        log.info("Verify request for phone: {}", maskPhone(request.getPhoneNumber()));

        AuthResponse response = cognitoService.confirmSignUp(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/signin")
    public ResponseEntity<AuthResponse> signIn(@Valid @RequestBody AuthRequest request) {
        log.info("Sign in request for phone: {}", maskPhone(request.getPhoneNumber()));

        AuthResponse response = cognitoService.signIn(request);

        if (response.isSuccess()) {
            // Get subscriber data
            Subscriber subscriber = subscriptionService.getSubscriberByPhoneNumber(request.getPhoneNumber());
            response.setSubscriber(subscriber);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/resend-code")
    public ResponseEntity<AuthResponse> resendCode(@RequestBody AuthRequest request) {
        log.info("Resend code request for phone: {}", maskPhone(request.getPhoneNumber()));

        AuthResponse response = cognitoService.resendConfirmationCode(request.getPhoneNumber());

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/complete-signup")
    public ResponseEntity<SubscriptionResponse> completeSignup(@Valid @RequestBody AuthRequest request) {
        log.info("Complete signup for phone: {} with {} coins",
                maskPhone(request.getPhoneNumber()),
                request.getSelectedCoins() != null ? request.getSelectedCoins().size() : 0);

        // Ensure BTC is included
        List<String> coins = request.getSelectedCoins();
        if (coins != null && !coins.contains("BTC")) {
            coins.add(0, "BTC");
        }

        SubscriptionResponse response = subscriptionService.subscribeWithCoins(
                request.getPhoneNumber(),
                request.getName(),
                request.getVerificationCode(), // Using verification code field to pass cognito user id
                coins
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/coins/popular")
    public ResponseEntity<List<CoinInfo>> getPopularCoins(
            @RequestParam(defaultValue = "100") int limit) {
        log.info("Fetching popular coins, limit: {}", limit);

        List<CoinInfo> coins = coinbaseService.getPopularCoins(limit);
        return ResponseEntity.ok(coins);
    }

    @PutMapping("/coins")
    public ResponseEntity<AuthResponse> updateCoins(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody AuthRequest request) {
        log.info("Update coins request for phone: {}", maskPhone(request.getPhoneNumber()));

        // Ensure BTC is included
        List<String> coins = request.getSelectedCoins();
        if (coins != null && !coins.contains("BTC")) {
            coins.add(0, "BTC");
        }

        boolean updated = subscriptionService.updateSelectedCoins(request.getPhoneNumber(), coins);

        if (updated) {
            Subscriber subscriber = subscriptionService.getSubscriberByPhoneNumber(request.getPhoneNumber());
            return ResponseEntity.ok(AuthResponse.builder()
                    .success(true)
                    .message("Coins updated successfully")
                    .subscriber(subscriber)
                    .build());
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AuthResponse.builder()
                .success(false)
                .message("Subscriber not found")
                .build());
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 2);
    }
}
