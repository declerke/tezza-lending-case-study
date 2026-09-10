package com.example.lending.domain.notification;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configurable rule that decides whether a given event should be sent, for which
 * product and/or customer segment, and on which channel. A null productId or
 * customerSegment means "applies to all". More specific rules take precedence
 * over general ones when several rules match the same event.
 */
@Entity
@Table(name = "notification_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private NotificationEventType eventType;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "customer_segment", length = 40)
    private String customerSegment;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private NotificationChannel channel;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    public int specificity() {
        if (productId != null && customerSegment != null) return 3;
        if (productId != null) return 2;
        if (customerSegment != null) return 1;
        return 0;
    }
}
