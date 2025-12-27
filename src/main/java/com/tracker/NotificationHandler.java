package com.tracker;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.tracker.service.NotificationService;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

public class NotificationHandler implements RequestHandler<Map<String, Object>, String> {

    private static ConfigurableApplicationContext applicationContext;
    private static NotificationService notificationService;

    static {
        applicationContext = SpringApplication.run(Application.class);
        notificationService = applicationContext.getBean(NotificationService.class);
    }

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("Received scheduled event: " + event);

        try {
            notificationService.sendPriceNotifications();
            return "Notification job completed successfully";
        } catch (Exception e) {
            context.getLogger().log("Error executing notification job: " + e.getMessage());
            throw new RuntimeException("Failed to send notifications", e);
        }
    }
}
