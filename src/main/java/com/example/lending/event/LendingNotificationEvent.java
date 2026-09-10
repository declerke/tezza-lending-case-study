package com.example.lending.event;

import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.loan.Loan;
import com.example.lending.domain.notification.NotificationEventType;
import java.util.Map;

public record LendingNotificationEvent(NotificationEventType type, Customer customer, Loan loan, Map<String,String> variables) {}
