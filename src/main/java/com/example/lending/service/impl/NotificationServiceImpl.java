package com.example.lending.service.impl;

import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.loan.Loan;
import com.example.lending.domain.notification.*;
import com.example.lending.dto.notification.*;
import com.example.lending.exception.BusinessException;
import com.example.lending.exception.ConflictException;
import com.example.lending.exception.ResourceNotFoundException;
import com.example.lending.notification.NotificationChannelProvider;
import com.example.lending.repository.*;
import com.example.lending.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private final NotificationTemplateRepository templates;
    private final NotificationRuleRepository rules;
    private final NotificationLogRepository logs;
    private final ProductRepository products;
    private final Map<NotificationChannel, NotificationChannelProvider> providers;

    public NotificationServiceImpl(NotificationTemplateRepository templates, NotificationRuleRepository rules,
                                   NotificationLogRepository logs, ProductRepository products,
                                   List<NotificationChannelProvider> channelProviders) {
        this.templates = templates;
        this.rules = rules;
        this.logs = logs;
        this.products = products;
        this.providers = new EnumMap<>(NotificationChannel.class);
        for (NotificationChannelProvider provider : channelProviders) {
            if (providers.putIfAbsent(provider.channel(), provider) != null) {
                throw new IllegalArgumentException("Duplicate notification provider for " + provider.channel());
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(NotificationEventType eventType, Customer customer, Loan loan, Map<String, String> variables) {
        NotificationChannel channel = resolveChannel(eventType, customer, loan);
        NotificationLog.NotificationLogBuilder audit = NotificationLog.builder()
                .customer(customer).loan(loan).eventType(eventType).channel(channel);
        Optional<NotificationTemplate> activeTemplate =
                templates.findByEventTypeAndChannelAndActiveTrue(eventType, channel);
        if (activeTemplate.isEmpty()) {
            logs.save(audit.body("No active template configured for " + eventType + " on " + channel)
                    .status(NotificationStatus.SKIPPED).build());
            return;
        }
        NotificationTemplate template = activeTemplate.orElseThrow();
        String subject = render(template.getSubjectTemplate(), customer, loan, variables);
        String body = render(template.getBodyTemplate(), customer, loan, variables);
        NotificationStatus status = deliver(channel, customer, subject, body, eventType);
        // Persistence errors must not be mistaken for delivery failures or retried in a rollback-only transaction.
        logs.save(audit.subject(subject).body(body).status(status).build());
    }

    private NotificationStatus deliver(NotificationChannel channel, Customer customer, String subject, String body,
                                       NotificationEventType eventType) {
        try {
            NotificationChannelProvider provider = providers.get(channel);
            if (provider == null) {
                throw new BusinessException("Unsupported notification channel " + channel);
            }
            String recipient = channel == NotificationChannel.EMAIL ? customer.getEmail() : customer.getPhone();
            provider.deliver(recipient, subject, body);
            return NotificationStatus.SENT;
        } catch (RuntimeException exception) {
            // The audit holds message content; operational logs do not need customer contact details.
            log.warn("Notification delivery failed for event {} on {} ({})",
                    eventType, channel, exception.getClass().getSimpleName());
            return NotificationStatus.FAILED;
        }
    }

    private NotificationChannel resolveChannel(NotificationEventType eventType, Customer customer, Loan loan) {
        Long productId = loan == null || loan.getProduct() == null ? null : loan.getProduct().getId();
        return rules.findByEventTypeAndEnabledTrue(eventType).stream()
                .filter(rule -> rule.getProductId() == null || rule.getProductId().equals(productId))
                .filter(rule -> rule.getCustomerSegment() == null
                        || rule.getCustomerSegment().equalsIgnoreCase(customer.getCustomerSegment()))
                .max(Comparator.comparingInt(NotificationRule::specificity)
                        .thenComparing(rule -> rule.getId() == null ? Long.MIN_VALUE : rule.getId()))
                .map(NotificationRule::getChannel).orElse(customer.getPreferredChannel());
    }

    private String render(String template, Customer customer, Loan loan, Map<String, String> variables) {
        if (template == null) {
            return null;
        }
        String message = template.replace("{customerName}", customer.getFirstName() + " " + customer.getLastName())
                .replace("{customerEmail}", customer.getEmail());
        if (loan != null) {
            message = message.replace("{loanNumber}", loan.getLoanNumber())
                    .replace("{outstandingPrincipal}", loan.getOutstandingPrincipal().toPlainString())
                    .replace("{outstandingFees}", loan.getOutstandingFees().toPlainString())
                    .replace("{dueDate}", loan.getDueDate() == null ? "N/A" : loan.getDueDate().toString());
        }
        if (variables != null) {
            for (Map.Entry<String, String> variable : variables.entrySet()) {
                message = message.replace("{" + variable.getKey() + "}", variable.getValue());
            }
        }
        // Unknown placeholders remain visible so template mistakes can be found in the audit.
        return message;
    }

    @Override
    public NotificationTemplateResponse createTemplate(NotificationTemplateRequest request) {
        if (templates.existsByCode(request.code())) {
            throw new BusinessException("Template code already exists");
        }
        NotificationTemplate template = NotificationTemplate.builder().code(request.code()).build();
        configureTemplate(template, request);
        validateActiveTemplateScope(template, null, template.isActive());
        return NotificationTemplateResponse.from(templates.save(template));
    }

    @Override
    public NotificationTemplateResponse updateTemplate(Long templateId, NotificationTemplateRequest request) {
        NotificationTemplate template = findTemplate(templateId);
        configureTemplate(template, request);
        validateActiveTemplateScope(template, templateId, template.isActive());
        return NotificationTemplateResponse.from(templates.save(template));
    }

    private void configureTemplate(NotificationTemplate template, NotificationTemplateRequest request) {
        // Code is an identity, so updates change only delivery configuration and content.
        template.setEventType(request.eventType());
        template.setChannel(request.channel());
        template.setSubjectTemplate(request.subjectTemplate());
        template.setBodyTemplate(request.bodyTemplate());
    }

    @Override
    public NotificationTemplateResponse setTemplateActive(Long templateId, boolean active) {
        NotificationTemplate template = findTemplate(templateId);
        if (active) {
            validateActiveTemplateScope(template, templateId, active);
        }
        template.setActive(active);
        return NotificationTemplateResponse.from(templates.save(template));
    }

    private void validateActiveTemplateScope(NotificationTemplate template, Long excludedId, boolean active) {
        if (!active) {
            return;
        }
        boolean conflict = excludedId == null
                ? templates.existsByEventTypeAndChannelAndActiveTrue(template.getEventType(), template.getChannel())
                : templates.existsByEventTypeAndChannelAndActiveTrueAndIdNot(
                        template.getEventType(), template.getChannel(), excludedId);
        if (conflict) {
            throw new ConflictException("An active template already exists for this event and channel.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> listTemplates() {
        return templates.findAll().stream().map(NotificationTemplateResponse::from).toList();
    }

    @Override
    public NotificationRuleResponse createRule(NotificationRuleRequest request) {
        NotificationRule rule = new NotificationRule();
        configureRule(rule, request);
        validateEnabledRuleScope(rule, null, rule.isEnabled());
        return NotificationRuleResponse.from(rules.save(rule));
    }

    @Override
    public NotificationRuleResponse updateRule(Long ruleId, NotificationRuleRequest request) {
        NotificationRule rule = findRule(ruleId);
        configureRule(rule, request);
        validateEnabledRuleScope(rule, ruleId, rule.isEnabled());
        return NotificationRuleResponse.from(rules.save(rule));
    }

    private void configureRule(NotificationRule rule, NotificationRuleRequest request) {
        if (request.productId() != null) {
            products.findById(request.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + request.productId()));
        }
        rule.setEventType(request.eventType());
        rule.setProductId(request.productId());
        rule.setCustomerSegment(normalizedSegment(request.customerSegment()));
        rule.setChannel(request.channel());
        rule.setEnabled(!Boolean.FALSE.equals(request.enabled()));
    }

    @Override
    public NotificationRuleResponse setRuleEnabled(Long ruleId, boolean enabled) {
        NotificationRule rule = findRule(ruleId);
        if (enabled) {
            validateEnabledRuleScope(rule, ruleId, enabled);
        }
        rule.setEnabled(enabled);
        return NotificationRuleResponse.from(rules.save(rule));
    }

    private void validateEnabledRuleScope(NotificationRule rule, Long excludedId, boolean enabled) {
        if (!enabled) {
            return;
        }
        if (rules.existsEnabledWithScope(rule.getEventType(), rule.getProductId(),
                normalizedSegment(rule.getCustomerSegment()), excludedId)) {
            throw new ConflictException("An enabled notification rule already exists for the same matching scope.");
        }
    }

    private String normalizedSegment(String customerSegment) {
        return customerSegment == null ? null : customerSegment.trim().toUpperCase(Locale.ROOT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationRuleResponse> listRules() {
        return rules.findAll().stream().map(NotificationRuleResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLogResponse> listLogsForCustomer(Long customerId) {
        return logs.findByCustomerId(customerId).stream().map(NotificationLogResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLogResponse> listLogsForLoan(Long loanId) {
        return logs.findByLoanId(loanId).stream().map(NotificationLogResponse::from).toList();
    }

    private NotificationTemplate findTemplate(Long templateId) {
        return templates.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification template not found with id " + templateId));
    }

    private NotificationRule findRule(Long ruleId) {
        return rules.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification rule not found with id " + ruleId));
    }
}
