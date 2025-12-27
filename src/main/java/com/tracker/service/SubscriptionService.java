package com.tracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tracker.model.Subscriber;
import com.tracker.model.SubscriberList;
import com.tracker.model.SubscriptionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class SubscriptionService {

    private static final String SUBSCRIBERS_KEY = "subscribers.json";

    private final S3Client s3Client;
    private final String bucketName;
    private final ObjectMapper objectMapper;

    public SubscriptionService(S3Client s3Client, @Qualifier("subscribersBucketName") String bucketName) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public SubscriptionResponse subscribe(String phoneNumber, String name) {
        log.info("Subscribing phone number: {} for {}", maskPhoneNumber(phoneNumber), name);

        SubscriberList subscriberList = getSubscriberList();

        // Check if already subscribed
        boolean alreadySubscribed = subscriberList.getSubscribers().stream()
                .anyMatch(s -> s.getPhoneNumber().equals(phoneNumber));

        if (alreadySubscribed) {
            log.info("Phone number already subscribed: {}", maskPhoneNumber(phoneNumber));
            return SubscriptionResponse.builder()
                    .phoneNumber(maskPhoneNumber(phoneNumber))
                    .message("Phone number is already subscribed")
                    .subscribedAt(Instant.now())
                    .build();
        }

        // Add new subscriber
        Subscriber newSubscriber = Subscriber.builder()
                .phoneNumber(phoneNumber)
                .name(name)
                .subscribedAt(Instant.now())
                .build();

        subscriberList.getSubscribers().add(newSubscriber);

        // Save to S3
        saveSubscriberList(subscriberList);

        log.info("Successfully subscribed phone number: {} for {}", maskPhoneNumber(phoneNumber), name);

        return SubscriptionResponse.builder()
                .phoneNumber(maskPhoneNumber(phoneNumber))
                .message("Successfully subscribed to crypto price notifications")
                .subscribedAt(newSubscriber.getSubscribedAt())
                .build();
    }

    public List<Subscriber> getAllSubscribers() {
        log.debug("Fetching all subscribers from S3");
        return getSubscriberList().getSubscribers();
    }

    private SubscriberList getSubscriberList() {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(SUBSCRIBERS_KEY)
                    .build();

            try (InputStream inputStream = s3Client.getObject(getRequest)) {
                return objectMapper.readValue(inputStream, SubscriberList.class);
            }
        } catch (NoSuchKeyException e) {
            log.info("No subscribers file found, creating new list");
            return new SubscriberList();
        } catch (Exception e) {
            log.error("Error reading subscribers from S3", e);
            return new SubscriberList();
        }
    }

    private void saveSubscriberList(SubscriberList subscriberList) {
        try {
            String json = objectMapper.writeValueAsString(subscriberList);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(SUBSCRIBERS_KEY)
                    .contentType("application/json")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromString(json));

            log.debug("Successfully saved {} subscribers to S3", subscriberList.getSubscribers().size());
        } catch (Exception e) {
            log.error("Error saving subscribers to S3", e);
            throw new RuntimeException("Failed to save subscription", e);
        }
    }

    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 2);
    }
}
