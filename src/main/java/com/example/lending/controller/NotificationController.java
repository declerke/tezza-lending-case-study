package com.example.lending.controller;

import com.example.lending.dto.notification.*;
import com.example.lending.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/templates")
    public ResponseEntity<NotificationTemplateResponse> createTemplate(@Valid @RequestBody NotificationTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createTemplate(request));
    }

    @GetMapping("/templates")
    public List<NotificationTemplateResponse> listTemplates() {
        return notificationService.listTemplates();
    }

    @PutMapping("/templates/{id}")
    public NotificationTemplateResponse updateTemplate(@PathVariable Long id, @Valid @RequestBody NotificationTemplateRequest request) { return notificationService.updateTemplate(id, request); }

    @PostMapping("/templates/{id}/activate")
    public NotificationTemplateResponse activateTemplate(@PathVariable Long id) { return notificationService.setTemplateActive(id, true); }

    @PostMapping("/templates/{id}/deactivate")
    public NotificationTemplateResponse deactivateTemplate(@PathVariable Long id) { return notificationService.setTemplateActive(id, false); }

    @PostMapping("/rules")
    public ResponseEntity<NotificationRuleResponse> createRule(@Valid @RequestBody NotificationRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.createRule(request));
    }

    @GetMapping("/rules")
    public List<NotificationRuleResponse> listRules() {
        return notificationService.listRules();
    }

    @PutMapping("/rules/{id}")
    public NotificationRuleResponse updateRule(@PathVariable Long id, @Valid @RequestBody NotificationRuleRequest request) { return notificationService.updateRule(id, request); }

    @PostMapping("/rules/{id}/enable")
    public NotificationRuleResponse enableRule(@PathVariable Long id) { return notificationService.setRuleEnabled(id, true); }

    @PostMapping("/rules/{id}/disable")
    public NotificationRuleResponse disableRule(@PathVariable Long id) { return notificationService.setRuleEnabled(id, false); }

    @GetMapping("/logs")
    public List<NotificationLogResponse> listLogs(@RequestParam(required = false) Long customerId,
                                                   @RequestParam(required = false) Long loanId) {
        if (customerId != null) {
            return notificationService.listLogsForCustomer(customerId);
        }
        if (loanId != null) {
            return notificationService.listLogsForLoan(loanId);
        }
        return List.of();
    }
}
