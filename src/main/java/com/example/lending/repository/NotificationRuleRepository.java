package com.example.lending.repository;

import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.domain.notification.NotificationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRuleRepository extends JpaRepository<NotificationRule, Long> {
    List<NotificationRule> findByEventTypeAndEnabledTrue(NotificationEventType eventType);

    @Query("""
            select count(r) > 0 from NotificationRule r
            where r.enabled = true
              and r.eventType = :eventType
              and ((:productId is null and r.productId is null) or r.productId = :productId)
              and ((:customerSegment is null and r.customerSegment is null)
                   or lower(r.customerSegment) = lower(:customerSegment))
              and (:excludedId is null or r.id <> :excludedId)
            """)
    boolean existsEnabledWithScope(@Param("eventType") NotificationEventType eventType,
                                   @Param("productId") Long productId,
                                   @Param("customerSegment") String customerSegment,
                                   @Param("excludedId") Long excludedId);
}
