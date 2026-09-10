package com.example.lending.dto.notification;

import com.example.lending.domain.notification.NotificationChannel;
import com.example.lending.domain.notification.NotificationEventType;
import jakarta.validation.constraints.NotNull;

public record NotificationRuleRequest(
        @NotNull NotificationEventType eventType,
        Long productId,
        String customerSegment,
        @NotNull NotificationChannel channel,
        Boolean enabled
) {
}
