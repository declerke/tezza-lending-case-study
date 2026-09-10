package com.example.lending.repository;

import com.example.lending.domain.notification.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByCustomerId(Long customerId);
    List<NotificationLog> findByLoanId(Long loanId);
}
