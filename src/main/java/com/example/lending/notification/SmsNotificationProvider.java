package com.example.lending.notification;

import com.example.lending.domain.notification.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsNotificationProvider implements NotificationChannelProvider {
    private static final Logger log = LoggerFactory.getLogger(SmsNotificationProvider.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void deliver(String recipient, String subject, String body) {
        log.info("Simulated SMS delivery completed");
    }
}
