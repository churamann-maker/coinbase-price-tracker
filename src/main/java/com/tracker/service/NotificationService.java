package com.tracker.service;

import com.tracker.model.PriceResponse;
import com.tracker.model.Subscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private static final List<String> SYMBOLS = Arrays.asList(
            "BTC", "ETH", "SOL", "ADA", "BNB", "XRP", "LINK", "ALGO"
    );

    private final SubscriptionService subscriptionService;
    private final CoinbaseService coinbaseService;
    private final VonageService vonageService;

    public void sendPriceNotifications() {
        log.info("Starting price notification job");

        List<Subscriber> subscribers = subscriptionService.getAllSubscribers();

        if (subscribers.isEmpty()) {
            log.info("No subscribers found, skipping notification");
            return;
        }

        log.info("Found {} subscribers to notify", subscribers.size());

        String priceDigest = buildPriceDigest();

        int successCount = 0;
        int failCount = 0;

        for (Subscriber subscriber : subscribers) {
            String personalizedMessage = buildPersonalizedMessage(subscriber, priceDigest);
            boolean sent = vonageService.sendSms(subscriber.getPhoneNumber(), personalizedMessage);
            if (sent) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("Notification job completed. Success: {}, Failed: {}", successCount, failCount);
    }

    private String buildPersonalizedMessage(Subscriber subscriber, String priceDigest) {
        String name = subscriber.getName() != null ? subscriber.getName() : "Subscriber";
        long daysSinceSubscribing = calculateDaysSinceSubscribing(subscriber.getSubscribedAt());

        StringBuilder message = new StringBuilder();
        message.append(String.format("Hi %s! ", name));
        message.append("You are well on your way to the path of crypto riches! ");
        message.append(String.format("Day %d of greatness! ", daysSinceSubscribing));
        message.append("Magic is in the work. Here is your SunCoin Digest:\n\n");
        message.append(priceDigest);

        return message.toString();
    }

    private long calculateDaysSinceSubscribing(Instant subscribedAt) {
        if (subscribedAt == null) {
            return 1;
        }
        long days = ChronoUnit.DAYS.between(subscribedAt, Instant.now());
        return days + 1; // Day 1 is the first day of subscription
    }

    private String buildPriceDigest() {
        StringBuilder message = new StringBuilder();

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

        for (String symbol : SYMBOLS) {
            try {
                PriceResponse prices = coinbaseService.getAllPrices(symbol);

                if (prices != null && prices.getSpotPrice() != null) {
                    String formattedPrice = formatPrice(prices.getSpotPrice(), currencyFormat);
                    message.append(String.format("%s: %s\n", symbol, formattedPrice));
                } else {
                    message.append(String.format("%s: N/A\n", symbol));
                }
            } catch (Exception e) {
                log.warn("Failed to get price for {}: {}", symbol, e.getMessage());
                message.append(String.format("%s: N/A\n", symbol));
            }
        }

        message.append("\n- CryptoTracker");

        return message.toString();
    }

    private String formatPrice(BigDecimal price, NumberFormat format) {
        if (price == null) {
            return "N/A";
        }
        return format.format(price);
    }
}
