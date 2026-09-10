package com.example.lending.notification;

import com.example.lending.domain.notification.NotificationChannel;

public interface NotificationChannelProvider {
    NotificationChannel channel();
    void deliver(String recipient, String subject, String body);
}
