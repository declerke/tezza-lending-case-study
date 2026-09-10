package com.example.lending.service;

import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.loan.Loan;
import com.example.lending.domain.notification.*;
import com.example.lending.domain.product.Product;
import com.example.lending.dto.notification.*;
import com.example.lending.exception.ResourceNotFoundException;
import com.example.lending.exception.ConflictException;
import com.example.lending.notification.NotificationChannelProvider;
import com.example.lending.repository.*;
import com.example.lending.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {
    @Mock NotificationTemplateRepository templates;
    @Mock NotificationRuleRepository rules;
    @Mock NotificationLogRepository logs;
    @Mock ProductRepository products;
    @Mock NotificationChannelProvider email;
    @Mock NotificationChannelProvider sms;
    @Mock NotificationChannelProvider push;
    private NotificationServiceImpl service;
    private Customer customer;
    private Loan loan;

    @BeforeEach
    void setUp() {
        when(email.channel()).thenReturn(NotificationChannel.EMAIL);
        when(sms.channel()).thenReturn(NotificationChannel.SMS);
        when(push.channel()).thenReturn(NotificationChannel.PUSH);
        service = new NotificationServiceImpl(templates, rules, logs, products, List.of(email, sms, push));
        customer = Customer.builder().id(1L).firstName("Amina").lastName("Otieno").email("amina@test")
                .phone("0700").preferredChannel(NotificationChannel.EMAIL).customerSegment("RETAIL").build();
        loan = Loan.builder().id(3L).loanNumber("LN-1").product(Product.builder().id(2L).build())
                .outstandingPrincipal(new BigDecimal("100")).outstandingFees(BigDecimal.ZERO).build();
    }

    @Test
    void rendersVariablesAndPreservesUnknownPlaceholders() {
        stubTemplate(NotificationChannel.EMAIL, "{loanNumber} {unknown} {amount}");

        service.notify(NotificationEventType.LOAN_CREATED, customer, loan, Map.of("amount", "20"));

        verify(email).deliver("amina@test", "Hello Amina Otieno", "LN-1 {unknown} 20");
        assertThat(savedLog().getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @ParameterizedTest
    @EnumSource(NotificationChannel.class)
    void resolvesProviderFromCustomerPreference(NotificationChannel channel) {
        customer.setPreferredChannel(channel);
        stubTemplate(channel, "body");

        service.notify(NotificationEventType.LOAN_CREATED, customer, loan, Map.of());

        NotificationChannelProvider provider = switch (channel) {
            case EMAIL -> email;
            case SMS -> sms;
            case PUSH -> push;
        };
        verify(provider).deliver(anyString(), anyString(), eq("body"));
        assertThat(savedLog().getChannel()).isEqualTo(channel);
    }

    @Test
    void recordsFailedDeliveryWithoutRetryingAuditPersistence() {
        stubTemplate(NotificationChannel.EMAIL, "body");
        doThrow(new IllegalStateException("simulated")).when(email).deliver(anyString(), any(), anyString());

        service.notify(NotificationEventType.LOAN_CREATED, customer, loan, Map.of());

        assertThat(savedLog().getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void propagatesAuditFailureInsteadOfMisclassifyingItAsDeliveryFailure() {
        stubTemplate(NotificationChannel.EMAIL, "body");
        when(logs.save(any())).thenThrow(new IllegalStateException("audit unavailable"));

        assertThatThrownBy(() -> service.notify(NotificationEventType.LOAN_CREATED, customer, loan, Map.of()))
                .isInstanceOf(IllegalStateException.class).hasMessage("audit unavailable");
        verify(logs, times(1)).save(any());
    }

    @Test
    void recordsSkippedWhenNoActiveTemplateExists() {
        service.notify(NotificationEventType.LOAN_CREATED, customer, loan, Map.of());

        assertThat(savedLog().getStatus()).isEqualTo(NotificationStatus.SKIPPED);
        verify(email, never()).deliver(any(), any(), any());
    }

    @Test
    void selectsMatchingProductAndSegmentRule() {
        NotificationRule matching = NotificationRule.builder().productId(2L).customerSegment("retail")
                .channel(NotificationChannel.SMS).enabled(true).build();
        NotificationRule unrelated = NotificationRule.builder().productId(99L)
                .channel(NotificationChannel.PUSH).enabled(true).build();
        when(rules.findByEventTypeAndEnabledTrue(NotificationEventType.LOAN_CREATED)).thenReturn(List.of(unrelated, matching));
        stubTemplate(NotificationChannel.SMS, "body");

        service.notify(NotificationEventType.LOAN_CREATED, customer, loan, Map.of());

        verify(sms).deliver(eq("0700"), anyString(), eq("body"));
    }

    @Test
    void updatesTemplateWithoutChangingCodeAndCanToggleActiveState() {
        NotificationTemplate template = NotificationTemplate.builder().id(4L).code("ORIGINAL").build();
        when(templates.findById(4L)).thenReturn(Optional.of(template));
        when(templates.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationTemplateResponse updated = service.updateTemplate(4L, new NotificationTemplateRequest(
                "RENAMED", NotificationEventType.LOAN_DISBURSED, NotificationChannel.SMS, "subject", "new body"));

        assertThat(updated.code()).isEqualTo("ORIGINAL");
        assertThat(updated.bodyTemplate()).isEqualTo("new body");
        assertThat(service.setTemplateActive(4L, false).active()).isFalse();
        assertThat(service.setTemplateActive(4L, true).active()).isTrue();
    }

    @Test
    void validatesProductAndDefaultsRuleToEnabled() {
        NotificationRule rule = NotificationRule.builder().id(8L).build();
        when(rules.findById(8L)).thenReturn(Optional.of(rule));
        when(rules.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(products.findById(2L)).thenReturn(Optional.of(Product.builder().id(2L).build()));

        NotificationRuleResponse updated = service.updateRule(8L, new NotificationRuleRequest(
                NotificationEventType.REPAYMENT_ACKNOWLEDGED, 2L, "RETAIL", NotificationChannel.PUSH, null));

        assertThat(updated.enabled()).isTrue();
        assertThat(service.setRuleEnabled(8L, false).enabled()).isFalse();
        assertThat(service.setRuleEnabled(8L, true).enabled()).isTrue();
    }

    @Test
    void rejectsRuleForMissingProduct() {
        assertThatThrownBy(() -> service.createRule(new NotificationRuleRequest(
                NotificationEventType.LOAN_CREATED, 99L, null, NotificationChannel.EMAIL, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(rules, never()).save(any());
    }

    @Test
    void rejectsDuplicateEnabledRuleScope() {
        NotificationRuleRequest request = new NotificationRuleRequest(
                NotificationEventType.LOAN_CREATED, 2L, " retail ", NotificationChannel.SMS, true);
        when(products.findById(2L)).thenReturn(Optional.of(Product.builder().id(2L).build()));
        when(rules.existsEnabledWithScope(NotificationEventType.LOAN_CREATED, 2L, "RETAIL", null))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createRule(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("An enabled notification rule already exists for the same matching scope.");
        verify(rules, never()).save(any());
    }

    @Test
    void allowsEquivalentDisabledRuleAndRejectsWhenItIsEnabled() {
        NotificationRule disabled = NotificationRule.builder().id(12L).eventType(NotificationEventType.LOAN_CREATED)
                .productId(2L).customerSegment("RETAIL").channel(NotificationChannel.SMS).enabled(false).build();
        when(rules.findById(12L)).thenReturn(Optional.of(disabled));
        when(rules.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(rules.existsEnabledWithScope(NotificationEventType.LOAN_CREATED, 2L, "RETAIL", 12L))
                .thenReturn(true);

        assertThat(service.setRuleEnabled(12L, false).enabled()).isFalse();
        assertThatThrownBy(() -> service.setRuleEnabled(12L, true))
                .isInstanceOf(ConflictException.class);
        verify(rules, times(1)).save(disabled);
    }

    @Test
    void rejectsUpdatingRuleIntoAnotherEnabledScopeButAllowsSelfUpdate() {
        NotificationRule existing = NotificationRule.builder().id(20L).eventType(NotificationEventType.LOAN_CREATED)
                .productId(2L).customerSegment("RETAIL").channel(NotificationChannel.EMAIL).enabled(true).build();
        NotificationRule current = NotificationRule.builder().id(21L).eventType(NotificationEventType.LOAN_CREATED)
                .productId(2L).customerSegment("RETAIL").channel(NotificationChannel.SMS).enabled(true).build();
        when(rules.findById(21L)).thenReturn(Optional.of(current));
        when(products.findById(2L)).thenReturn(Optional.of(Product.builder().id(2L).build()));
        when(rules.existsEnabledWithScope(NotificationEventType.LOAN_CREATED, 2L, "RETAIL", 21L))
                .thenReturn(true);

        assertThatThrownBy(() -> service.updateRule(21L, new NotificationRuleRequest(
                NotificationEventType.LOAN_CREATED, 2L, "RETAIL", NotificationChannel.PUSH, true)))
                .isInstanceOf(ConflictException.class);

        when(rules.existsEnabledWithScope(NotificationEventType.LOAN_CREATED, 2L, "RETAIL", 21L))
                .thenReturn(false);
        when(rules.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.updateRule(21L, new NotificationRuleRequest(
                NotificationEventType.LOAN_CREATED, 2L, "RETAIL", NotificationChannel.PUSH, true)).channel())
                .isEqualTo(NotificationChannel.PUSH);
        verify(rules, times(1)).save(current);
    }

    @Test
    void allowsDifferentSpecificityRulesAndUsesTheMoreSpecificRule() {
        NotificationRule generic = NotificationRule.builder().id(1L).eventType(NotificationEventType.LOAN_CREATED)
                .channel(NotificationChannel.EMAIL).enabled(true).build();
        NotificationRule productRule = NotificationRule.builder().id(2L).eventType(NotificationEventType.LOAN_CREATED)
                .productId(2L).channel(NotificationChannel.SMS).enabled(true).build();
        when(rules.findByEventTypeAndEnabledTrue(NotificationEventType.LOAN_CREATED))
                .thenReturn(List.of(generic, productRule));
        stubTemplate(NotificationChannel.SMS, "body");

        service.notify(NotificationEventType.LOAN_CREATED, customer, loan, Map.of());

        verify(sms).deliver(eq("0700"), anyString(), eq("body"));
    }

    @Test
    void rejectsDuplicateActiveTemplateAndAllowsReplacementAfterDeactivation() {
        NotificationTemplateRequest request = new NotificationTemplateRequest(
                "SECOND", NotificationEventType.LOAN_CREATED, NotificationChannel.EMAIL, "subject", "body");
        when(templates.existsByCode("SECOND")).thenReturn(false);
        when(templates.existsByEventTypeAndChannelAndActiveTrue(NotificationEventType.LOAN_CREATED, NotificationChannel.EMAIL))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createTemplate(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("An active template already exists for this event and channel.");

        NotificationTemplate current = NotificationTemplate.builder().id(31L).code("CURRENT")
                .eventType(NotificationEventType.LOAN_CREATED).channel(NotificationChannel.EMAIL)
                .bodyTemplate("old").active(true).build();
        NotificationTemplate replacement = NotificationTemplate.builder().id(32L).code("REPLACEMENT")
                .eventType(NotificationEventType.LOAN_CREATED).channel(NotificationChannel.EMAIL)
                .bodyTemplate("new").active(false).build();
        when(templates.findById(31L)).thenReturn(Optional.of(current));
        when(templates.findById(32L)).thenReturn(Optional.of(replacement));
        when(templates.existsByEventTypeAndChannelAndActiveTrueAndIdNot(NotificationEventType.LOAN_CREATED,
                NotificationChannel.EMAIL, 32L)).thenReturn(true, false);
        when(templates.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.setTemplateActive(32L, true))
                .isInstanceOf(ConflictException.class);
        assertThat(service.setTemplateActive(31L, false).active()).isFalse();
        assertThat(service.setTemplateActive(32L, true).active()).isTrue();
    }

    @Test
    void rejectsUpdatingActiveTemplateIntoOccupiedScope() {
        NotificationTemplate current = NotificationTemplate.builder().id(41L).code("CURRENT")
                .eventType(NotificationEventType.LOAN_CREATED).channel(NotificationChannel.EMAIL)
                .bodyTemplate("old").active(true).build();
        when(templates.findById(41L)).thenReturn(Optional.of(current));
        when(templates.existsByEventTypeAndChannelAndActiveTrueAndIdNot(NotificationEventType.LOAN_DISBURSED,
                NotificationChannel.SMS, 41L)).thenReturn(true);

        assertThatThrownBy(() -> service.updateTemplate(41L, new NotificationTemplateRequest(
                "IGNORED", NotificationEventType.LOAN_DISBURSED, NotificationChannel.SMS, "subject", "body")))
                .isInstanceOf(ConflictException.class);
        verify(templates, never()).save(any());
    }

    private void stubTemplate(NotificationChannel channel, String body) {
        NotificationTemplate template = NotificationTemplate.builder().eventType(NotificationEventType.LOAN_CREATED)
                .channel(channel).subjectTemplate("Hello {customerName}").bodyTemplate(body).active(true).build();
        when(templates.findByEventTypeAndChannelAndActiveTrue(NotificationEventType.LOAN_CREATED, channel))
                .thenReturn(Optional.of(template));
    }

    private NotificationLog savedLog() {
        ArgumentCaptor<NotificationLog> audit = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logs).save(audit.capture());
        return audit.getValue();
    }
}
