package com.example.lending.scheduler;

import com.example.lending.config.LendingProperties;
import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.loan.*;
import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.domain.product.*;
import com.example.lending.event.LendingNotificationEvent;
import com.example.lending.repository.*;
import com.example.lending.service.FeeCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SweepJobServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 8);
    @Mock LoanRepository loans;
    @Mock LoanChargeRepository charges;
    @Mock ApplicationEventPublisher events;
    private SweepJobService sweep;
    private Loan loan;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder().id(1L).firstName("A").lastName("B").build();
        Product product = Product.builder().id(1L).fees(new ArrayList<>()).build();
        loan = Loan.builder().id(1L).loanNumber("LN-1").customer(customer).product(product)
                .state(LoanState.OPEN).outstandingPrincipal(new BigDecimal("1000"))
                .outstandingFees(BigDecimal.ZERO).build();
        when(loans.findByStateIn(List.of(LoanState.OPEN, LoanState.OVERDUE))).thenReturn(List.of(loan));
        sweep = new SweepJobService(loans, charges, new FeeCalculationService(), events,
                Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC),
                new LendingProperties(new LendingProperties.SweepJob("*"), new LendingProperties.Notifications(1)));
    }

    @ParameterizedTest(name = "{0} days past due: late fee eligible = {1}")
    @CsvSource({"2,false", "3,true", "4,true", "10,true"})
    void appliesLateFeeAtOrAfterThreshold(int daysPastDue, boolean eligible) {
        loan.getProduct().getFees().add(fee(3L, FeeCategory.LATE_FEE, "30", 3));
        Installment installment = installmentDue(TODAY.minusDays(daysPastDue));

        sweep.runSweep();

        assertThat(loan.getState()).isEqualTo(LoanState.OVERDUE);
        assertThat(installment.getState()).isEqualTo(InstallmentState.OVERDUE);
        assertThat(loan.getCharges()).hasSize(eligible ? 1 : 0);
        assertThat(installment.getFeesDue()).isEqualByComparingTo(eligible ? "30" : "0");
    }

    @Test
    void appliesMissedThresholdAndIndependentLateTiersOnlyOnce() {
        loan.getProduct().getFees().addAll(List.of(
                fee(3L, FeeCategory.LATE_FEE, "30", 3),
                fee(7L, FeeCategory.LATE_FEE, "70", 7)));
        Installment installment = installmentDue(TODAY.minusDays(7));
        stubPersistedChargeLookup();

        sweep.runSweep();
        sweep.runSweep();

        assertThat(loan.getCharges()).hasSize(2).allSatisfy(charge ->
                assertThat(charge.getInstallment()).isSameAs(installment));
        assertThat(installment.getFeesDue()).isEqualByComparingTo("100");
        assertThat(loan.totalOutstanding()).isEqualByComparingTo("1100");
        ArgumentCaptor<LendingNotificationEvent> event = ArgumentCaptor.forClass(LendingNotificationEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo(NotificationEventType.OVERDUE_NOTICE);
    }

    @Test
    void doesNotApplyDailyFeeTwiceOnTheSameDay() {
        loan.getProduct().getFees().add(fee(9L, FeeCategory.DAILY_FEE, "5", null));
        installmentDue(TODAY.plusDays(10));
        stubPersistedChargeLookup();

        sweep.runSweep();
        sweep.runSweep();

        assertThat(loan.getCharges()).hasSize(1);
        assertThat(loan.getOutstandingFees()).isEqualByComparingTo("5");
    }

    @Test
    void publishesReminderInConfiguredWindow() {
        installmentDue(TODAY.plusDays(1));

        sweep.runSweep();

        ArgumentCaptor<LendingNotificationEvent> event = ArgumentCaptor.forClass(LendingNotificationEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo(NotificationEventType.DUE_DATE_REMINDER);
    }

    private Installment installmentDue(LocalDate dueDate) {
        Installment installment = Installment.builder().installmentNumber(1).dueDate(dueDate)
                .principalDue(new BigDecimal("1000")).build();
        loan.addInstallment(installment);
        return installment;
    }

    private Fee fee(Long id, FeeCategory category, String amount, Integer daysAfterDue) {
        return Fee.builder().id(id).feeCategory(category).calculationType(FeeCalculationType.FIXED)
                .amount(new BigDecimal(amount)).daysAfterDue(daysAfterDue).active(true).build();
    }

    private void stubPersistedChargeLookup() {
        Set<String> persistedKeys = new HashSet<>();
        when(charges.existsByDeduplicationKey(anyString())).thenAnswer(invocation ->
                persistedKeys.contains(invocation.getArgument(0)));
        when(loans.save(any(Loan.class))).thenAnswer(invocation -> {
            Loan saved = invocation.getArgument(0);
            saved.getCharges().forEach(charge -> persistedKeys.add(charge.getDeduplicationKey()));
            return saved;
        });
    }
}
