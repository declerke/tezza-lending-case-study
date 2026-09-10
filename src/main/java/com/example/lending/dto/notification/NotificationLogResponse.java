package com.example.lending.dto.notification;

import com.example.lending.domain.notification.NotificationChannel;
import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.domain.notification.NotificationLog;
import com.example.lending.domain.notification.NotificationStatus;

import java.time.Instant;

public record NotificationLogResponse(
        Long id,
        Long customerId,
        Long loanId,
        NotificationEventType eventType,
        NotificationChannel channel,
        String subject,
        String body,
        NotificationStatus status,
        Instant sentAt
) {
    public static NotificationLogResponse from(NotificationLog log) {
        return new NotificationLogResponse(
                log.getId(),
                log.getCustomer().getId(),
                log.getLoan() != null ? log.getLoan().getId() : null,
                log.getEventType(),
                log.getChannel(),
                log.getSubject(),
                log.getBody(),
                log.getStatus(),
                log.getSentAt()
        );
    }
}
