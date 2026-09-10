package com.example.lending.dto.customer;

import com.example.lending.domain.notification.NotificationChannel;
import jakarta.validation.constraints.NotNull;

public record CustomerPreferenceUpdateRequest(@NotNull NotificationChannel preferredChannel) {
}
