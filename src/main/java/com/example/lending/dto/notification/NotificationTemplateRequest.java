package com.example.lending.dto.notification;

import com.example.lending.domain.notification.NotificationChannel;
import com.example.lending.domain.notification.NotificationEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationTemplateRequest(
        @NotBlank String code,
        @NotNull NotificationEventType eventType,
        @NotNull NotificationChannel channel,
        String subjectTemplate,
        @NotBlank String bodyTemplate
) {
}
