package com.example.lending.scheduler;

import com.example.lending.config.LendingProperties;
import com.example.lending.domain.loan.*;
import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.domain.product.Fee;
import com.example.lending.domain.product.FeeCategory;
import com.example.lending.event.LendingNotificationEvent;
import com.example.lending.repository.LoanChargeRepository;
import com.example.lending.repository.LoanRepository;
import com.example.lending.service.FeeCalculationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Service
public class SweepJobService {
    private final LoanRepository loans;
    private final LoanChargeRepository charges;
    private final FeeCalculationService feeCalculator;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final int reminderDays;

    public SweepJobService(LoanRepository loans, LoanChargeRepository charges, FeeCalculationService feeCalculator,
                           ApplicationEventPublisher events, Clock clock, LendingProperties properties) {
        this.loans = loans;
        this.charges = charges;
        this.feeCalculator = feeCalculator;
        this.events = events;
        this.clock = clock;
        this.reminderDays = properties.notifications().dueSoonReminderDays();
    }

    @Transactional
    public void runSweep() {
        LocalDate today = LocalDate.now(clock);
        for (Loan loan : loans.findByStateIn(List.of(LoanState.OPEN, LoanState.OVERDUE))) {
            sendDueDateReminder(loan, today);
            processOverdueInstallments(loan, today);
            accrueDailyFees(loan, today);
            loans.save(loan);
        }
    }

    private void sendDueDateReminder(Loan loan, LocalDate today) {
        LocalDate reminderDueDate = today.plusDays(reminderDays);
        boolean dueSoon = loan.getInstallments().stream()
                .anyMatch(installment -> installment.getState() == InstallmentState.PENDING
                        && installment.getDueDate().equals(reminderDueDate));
        if (dueSoon) {
            publish(NotificationEventType.DUE_DATE_REMINDER, loan);
        }
    }

    private void processOverdueInstallments(Loan loan, LocalDate today) {
        boolean becameOverdue = false;
        for (Installment installment : loan.getInstallments()) {
            if (!installment.isOverdueOn(today)) {
                continue;
            }
            if (installment.getState() == InstallmentState.PENDING) {
                installment.setState(InstallmentState.OVERDUE);
                becameOverdue = true;
            }
            applyLateFees(loan, installment, today);
        }
        if (becameOverdue) {
            loan.setState(LoanState.OVERDUE);
            publish(NotificationEventType.OVERDUE_NOTICE, loan);
        }
    }

    private void applyLateFees(Loan loan, Installment installment, LocalDate today) {
        long daysPastDue = ChronoUnit.DAYS.between(installment.getDueDate(), today);
        for (Fee fee : loan.getProduct().getFees()) {
            if (!fee.isActive() || fee.getFeeCategory() != FeeCategory.LATE_FEE
                    || fee.getDaysAfterDue() == null || daysPastDue < fee.getDaysAfterDue()) {
                continue;
            }
            // A tier belongs to an installment, not the date the sweep happened to run.
            String key = loan.getLoanNumber() + ":LATE:" + installment.getInstallmentNumber() + ":" + fee.getId();
            applyChargeIfMissing(loan, installment, fee, today, key,
                    "Late fee applied after " + daysPastDue + " day(s) past due");
        }
    }

    private void accrueDailyFees(Loan loan, LocalDate today) {
        for (Fee fee : loan.getProduct().getFees()) {
            if (fee.isActive() && fee.getFeeCategory() == FeeCategory.DAILY_FEE) {
                String key = loan.getLoanNumber() + ":DAILY:" + today + ":" + fee.getId();
                applyChargeIfMissing(loan, null, fee, today, key, "Daily fee accrual");
            }
        }
    }

    private void applyChargeIfMissing(Loan loan, Installment installment, Fee fee, LocalDate date,
                                      String key, String description) {
        if (charges.existsByDeduplicationKey(key)) {
            return;
        }
        loan.applyCharge(LoanCharge.builder().installment(installment).feeConfiguration(fee)
                .feeCategory(fee.getFeeCategory()).amount(feeCalculator.calculate(fee, loan.getOutstandingPrincipal()))
                .appliedDate(date).deduplicationKey(key).description(description).build());
    }

    private void publish(NotificationEventType eventType, Loan loan) {
        events.publishEvent(new LendingNotificationEvent(eventType, loan.getCustomer(), loan, Map.of()));
    }
}
