package com.example.lending.integration;

import com.example.lending.domain.notification.NotificationStatus;
import com.example.lending.dto.loan.LoanCreateRequest;
import com.example.lending.dto.loan.LoanResponse;
import com.example.lending.dto.notification.NotificationRuleRequest;
import com.example.lending.dto.notification.NotificationRuleResponse;
import com.example.lending.dto.notification.NotificationTemplateRequest;
import com.example.lending.dto.notification.NotificationTemplateResponse;
import com.example.lending.domain.notification.NotificationChannel;
import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.notification.EmailNotificationProvider;
import com.example.lending.repository.LoanRepository;
import com.example.lending.repository.NotificationLogRepository;
import com.example.lending.repository.NotificationRuleRepository;
import com.example.lending.repository.NotificationTemplateRepository;
import com.example.lending.service.LoanService;
import com.example.lending.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.annotation.DirtiesContext;

import javax.sql.DataSource;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:transaction-hardening;DB_CLOSE_DELAY=0",
        "lending.sweep-job.cron=-"
})
class LendingTransactionTest {
    @Autowired LoanService loanService;
    @Autowired LoanRepository loans;
    @Autowired NotificationLogRepository notifications;
    @Autowired NotificationRuleRepository rules;
    @Autowired NotificationTemplateRepository templates;
    @Autowired NotificationService notificationService;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @SpyBean EmailNotificationProvider email;

    @Test
    void recordsFailedNotificationAfterCommitWithoutRollingBackLoan() {
        doThrow(new IllegalStateException("simulated delivery failure"))
                .when(email).deliver(anyString(), any(), anyString());
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        LoanResponse created = transaction.execute(status -> {
            LoanResponse pending = loanService.createLoan(
                    new LoanCreateRequest(2L, 1L, new BigDecimal("1000"), 10));
            assertThat(notifications.findByLoanId(pending.id())).isEmpty();
            return pending;
        });

        assertThat(created).isNotNull();
        assertThat(loans.existsById(created.id())).isTrue();
        assertThat(notifications.findByLoanId(created.id()))
                .singleElement().satisfies(audit -> assertThat(audit.getStatus()).isEqualTo(NotificationStatus.FAILED));
    }

    @Test
    void doesNotDeliverNotificationsForRolledBackLoan() {
        long loansBefore = loans.count();
        long notificationsBefore = notifications.count();
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        transaction.executeWithoutResult(status -> {
            loanService.createLoan(new LoanCreateRequest(2L, 1L, new BigDecimal("1000"), 10));
            status.setRollbackOnly();
        });

        assertThat(loans.count()).isEqualTo(loansBefore);
        assertThat(notifications.count()).isEqualTo(notificationsBefore);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void rerunningSeedsPreservesExistingLimitsAndNotificationMaintenance() {
        jdbc.update("UPDATE loan_limits SET available_limit = ?, version = version + 1 WHERE id = 1", new BigDecimal("12345"));
        jdbc.update("UPDATE notification_templates SET active = FALSE, body_template = 'Maintained content' WHERE id = 1");
        jdbc.update("UPDATE notification_rules SET enabled = FALSE WHERE id = 1");
        Long version = jdbc.queryForObject("SELECT version FROM loan_limits WHERE id = 1", Long.class);

        ResourceDatabasePopulator seeds = new ResourceDatabasePopulator(
                new ClassPathResource("database/seed/001_reference_data.sql"),
                new ClassPathResource("database/seed/002_demo_data.sql"));
        seeds.execute(dataSource);

        assertThat(jdbc.queryForObject("SELECT available_limit FROM loan_limits WHERE id = 1", BigDecimal.class))
                .isEqualByComparingTo("12345");
        assertThat(jdbc.queryForObject("SELECT version FROM loan_limits WHERE id = 1", Long.class)).isEqualTo(version);
        assertThat(jdbc.queryForObject("SELECT active FROM notification_templates WHERE id = 1", Boolean.class)).isFalse();
        assertThat(jdbc.queryForObject("SELECT body_template FROM notification_templates WHERE id = 1", String.class))
                .isEqualTo("Maintained content");
        assertThat(jdbc.queryForObject("SELECT enabled FROM notification_rules WHERE id = 1", Boolean.class)).isFalse();
    }

    @Test
    void enforcesCaseInsensitiveEnabledRuleScopeWithRealRepositoryQuery() {
        NotificationRuleResponse first = notificationService.createRule(new NotificationRuleRequest(
                NotificationEventType.LOAN_CLOSED, null, "Retail", NotificationChannel.PUSH, true));

        try {
            assertThatThrownBy(() -> notificationService.createRule(new NotificationRuleRequest(
                    NotificationEventType.LOAN_CLOSED, null, "RETAIL", NotificationChannel.EMAIL, true)))
                    .isInstanceOf(com.example.lending.exception.ConflictException.class);
        } finally {
            rules.deleteById(first.id());
        }
    }

    @Test
    void enforcesOneActiveTemplatePerEventAndChannelWithRealRepositoryQuery() {
        NotificationTemplateResponse first = notificationService.createTemplate(new NotificationTemplateRequest(
                "INTEGRATION_WRITTEN_OFF_PUSH_1", NotificationEventType.LOAN_WRITTEN_OFF,
                NotificationChannel.PUSH, null, "first"));

        try {
            assertThatThrownBy(() -> notificationService.createTemplate(new NotificationTemplateRequest(
                    "INTEGRATION_WRITTEN_OFF_PUSH_2", NotificationEventType.LOAN_WRITTEN_OFF,
                    NotificationChannel.PUSH, null, "second")))
                    .isInstanceOf(com.example.lending.exception.ConflictException.class);
        } finally {
            templates.deleteById(first.id());
        }
    }
}
