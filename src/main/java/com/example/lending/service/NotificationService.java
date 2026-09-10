package com.example.lending.service;

import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.loan.Loan;
import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.dto.notification.NotificationLogResponse;
import com.example.lending.dto.notification.NotificationRuleRequest;
import com.example.lending.dto.notification.NotificationRuleResponse;
import com.example.lending.dto.notification.NotificationTemplateRequest;
import com.example.lending.dto.notification.NotificationTemplateResponse;

import java.util.List;
import java.util.Map;

public interface NotificationService {

    void notify(NotificationEventType eventType, Customer customer, Loan loan, Map<String, String> variables);

    NotificationTemplateResponse createTemplate(NotificationTemplateRequest request);
    NotificationTemplateResponse updateTemplate(Long id, NotificationTemplateRequest request);
    NotificationTemplateResponse setTemplateActive(Long id, boolean active);
    List<NotificationTemplateResponse> listTemplates();

    NotificationRuleResponse createRule(NotificationRuleRequest request);
    NotificationRuleResponse updateRule(Long id, NotificationRuleRequest request);
    NotificationRuleResponse setRuleEnabled(Long id, boolean enabled);
    List<NotificationRuleResponse> listRules();

    List<NotificationLogResponse> listLogsForCustomer(Long customerId);
    List<NotificationLogResponse> listLogsForLoan(Long loanId);
}
