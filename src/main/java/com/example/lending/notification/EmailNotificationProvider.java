package com.example.lending.notification;

import com.example.lending.domain.notification.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationProvider implements NotificationChannelProvider {
    private static final Logger log = LoggerFactory.getLogger(EmailNotificationProvider.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void deliver(String recipient, String subject, String body) {
        log.info("Simulated EMAIL delivery completed");
    }
}
