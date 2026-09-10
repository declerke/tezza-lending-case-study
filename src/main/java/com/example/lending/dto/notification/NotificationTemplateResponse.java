package com.example.lending.dto.notification;

import com.example.lending.domain.notification.NotificationChannel;
import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.domain.notification.NotificationTemplate;

public record NotificationTemplateResponse(
        Long id,
        String code,
        NotificationEventType eventType,
        NotificationChannel channel,
        String subjectTemplate,
        String bodyTemplate,
        boolean active
) {
    public static NotificationTemplateResponse from(NotificationTemplate template) {
        return new NotificationTemplateResponse(
                template.getId(),
                template.getCode(),
                template.getEventType(),
                template.getChannel(),
                template.getSubjectTemplate(),
                template.getBodyTemplate(),
                template.isActive()
        );
    }
}
