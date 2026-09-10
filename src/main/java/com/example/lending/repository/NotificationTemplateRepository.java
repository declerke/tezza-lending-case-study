package com.example.lending.repository;

import com.example.lending.domain.notification.NotificationChannel;
import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.domain.notification.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    boolean existsByCode(String code);
    boolean existsByEventTypeAndChannelAndActiveTrue(NotificationEventType eventType, NotificationChannel channel);
    boolean existsByEventTypeAndChannelAndActiveTrueAndIdNot(NotificationEventType eventType,
                                                               NotificationChannel channel,
                                                               Long id);
    Optional<NotificationTemplate> findByEventTypeAndChannelAndActiveTrue(NotificationEventType eventType, NotificationChannel channel);
}
