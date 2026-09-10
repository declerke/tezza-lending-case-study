package com.example.lending.event;

import com.example.lending.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class NotificationEventListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private final NotificationService notifications;

    public NotificationEventListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(LendingNotificationEvent event) {
        try {
            // notify owns a separate transaction. This catch includes its commit failures.
            notifications.notify(event.type(), event.customer(), event.loan(), event.variables());
        } catch (RuntimeException exception) {
            log.error("Notification processing failed for {}", event.type(), exception);
        }
    }
}
