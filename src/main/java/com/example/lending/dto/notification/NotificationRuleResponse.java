package com.example.lending.dto.notification;

import com.example.lending.domain.notification.NotificationChannel;
import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.domain.notification.NotificationRule;

public record NotificationRuleResponse(
        Long id,
        NotificationEventType eventType,
        Long productId,
        String customerSegment,
        NotificationChannel channel,
        boolean enabled
) {
    public static NotificationRuleResponse from(NotificationRule rule) {
        return new NotificationRuleResponse(
                rule.getId(),
                rule.getEventType(),
                rule.getProductId(),
                rule.getCustomerSegment(),
                rule.getChannel(),
                rule.isEnabled()
        );
    }
}
