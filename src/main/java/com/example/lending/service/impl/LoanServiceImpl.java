package com.example.lending.service.impl;

import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.customer.LoanLimit;
import com.example.lending.domain.loan.*;
import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.domain.product.*;
import com.example.lending.dto.loan.*;
import com.example.lending.event.LendingNotificationEvent;
import com.example.lending.exception.BusinessException;
import com.example.lending.exception.ResourceNotFoundException;
import com.example.lending.repository.*;
import com.example.lending.service.FeeCalculationService;
import com.example.lending.service.LoanService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class LoanServiceImpl implements LoanService {
    private final LoanRepository loans;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final LoanLimitRepository limits;
    private final LoanChargeRepository charges;
    private final FeeCalculationService feeCalculator;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LoanServiceImpl(LoanRepository loans, ProductRepository products, CustomerRepository customers,
                           LoanLimitRepository limits, LoanChargeRepository charges,
                           FeeCalculationService feeCalculator, ApplicationEventPublisher events, Clock clock) {
        this.loans = loans;
        this.products = products;
        this.customers = customers;
        this.limits = limits;
        this.charges = charges;
        this.feeCalculator = feeCalculator;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public LoanResponse createAndDisburseLoan(LoanCreateRequest request) {
        return disburse(createPendingLoan(request));
    }

    @Override
    public LoanResponse createLoan(LoanCreateRequest request) {
        return LoanResponse.from(createPendingLoan(request));
    }

    private Loan createPendingLoan(LoanCreateRequest request) {
        Customer customer = customers.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + request.customerId()));
        Product product = products.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + request.productId()));
        validateProduct(product, request.tenureValue());
        if (resolveBillingCycle(customer, product) == BillingCycleType.CONSOLIDATED
                && customer.getBillingDay() == null) {
            throw new BusinessException("Consolidated billing requires a customer billingDay");
        }
        requireAvailableLimit(customer.getId(), request.principalAmount());

        Loan loan = Loan.builder()
                .loanNumber("LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT))
                .customer(customer).product(product).principalAmount(request.principalAmount())
                .interestRate(product.getInterestRate()).tenureValue(request.tenureValue())
                .loanStructureType(product.getLoanStructureType())
                .billingCycleType(resolveBillingCycle(customer, product)).state(LoanState.PENDING)
                .outstandingPrincipal(request.principalAmount()).outstandingFees(BigDecimal.ZERO)
                .createdAt(Instant.now(clock)).build();
        applyServiceFees(loan, FeeApplicationTiming.ORIGINATION);
        Loan saved = loans.save(loan);
        publish(NotificationEventType.LOAN_CREATED, saved, Map.of());
        return saved;
    }

    @Override
    public LoanResponse disburseLoan(Long loanId) {
        return disburse(findLoan(loanId));
    }

    private LoanResponse disburse(Loan loan) {
        if (loan.getState() != LoanState.PENDING) {
            throw new BusinessException("Only PENDING loans can be disbursed");
        }
        LoanLimit loanLimit = requireAvailableLimit(loan.getCustomer().getId(), loan.getPrincipalAmount());
        LocalDate disbursementDate = LocalDate.now(clock);
        LocalDate dueDate = resolveDueDate(loan, disbursementDate);
        loan.setDisbursementDate(disbursementDate);
        loan.setDueDate(dueDate);
        loan.setState(LoanState.OPEN);
        buildSchedule(loan, disbursementDate, dueDate);
        applyServiceFees(loan, FeeApplicationTiming.POST_DISBURSEMENT);
        loanLimit.reserve(loan.getPrincipalAmount(), Instant.now(clock));
        Loan saved = loans.save(loan);
        publish(NotificationEventType.LOAN_DISBURSED, saved, Map.of());
        return LoanResponse.from(saved);
    }

    private void validateProduct(Product product, int tenure) {
        if (!product.isActive()) {
            throw new BusinessException("Product '" + product.getCode() + "' is not active");
        }
        if (tenure < product.getMinTenureValue() || tenure > product.getMaxTenureValue()) {
            throw new BusinessException("Tenure value must be between " + product.getMinTenureValue()
                    + " and " + product.getMaxTenureValue() + " " + product.getTenureType());
        }
    }

    private LoanLimit requireAvailableLimit(Long customerId, BigDecimal amount) {
        LoanLimit loanLimit = findLimit(customerId);
        if (loanLimit.getAvailableLimit().compareTo(amount) < 0) {
            throw new BusinessException("Requested principal exceeds customer's available loan limit");
        }
        return loanLimit;
    }

    private BillingCycleType resolveBillingCycle(Customer customer, Product product) {
        if (customer.getBillingCycleType() == BillingCycleType.CONSOLIDATED
                || product.getBillingCycleType() == BillingCycleType.CONSOLIDATED) {
            return BillingCycleType.CONSOLIDATED;
        }
        return BillingCycleType.INDIVIDUAL_DUE_DATE;
    }

    private LocalDate resolveDueDate(Loan loan, LocalDate disbursementDate) {
        Product product = loan.getProduct();
        LocalDate individualDueDate = product.getTenureType() == TenureType.DAYS
                ? disbursementDate.plusDays(loan.getTenureValue())
                : disbursementDate.plusMonths(loan.getTenureValue());
        Customer customer = loan.getCustomer();
        if (resolveBillingCycle(customer, product) == BillingCycleType.INDIVIDUAL_DUE_DATE) {
            return individualDueDate;
        }
        return loans.findByCustomerId(customer.getId()).stream()
                .filter(Loan::hasActiveExposure).map(Loan::getDueDate).filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> billingDate(individualDueDate, customer.getBillingDay(), disbursementDate));
    }

    private LocalDate billingDate(LocalDate tenureDueDate, int billingDay, LocalDate disbursementDate) {
        LocalDate aligned = tenureDueDate.withDayOfMonth(Math.min(billingDay, tenureDueDate.lengthOfMonth()));
        if (aligned.isBefore(disbursementDate)) {
            LocalDate nextMonth = tenureDueDate.plusMonths(1);
            return nextMonth.withDayOfMonth(Math.min(billingDay, nextMonth.lengthOfMonth()));
        }
        return aligned;
    }

    private void buildSchedule(Loan loan, LocalDate start, LocalDate dueDate) {
        Product product = loan.getProduct();
        int count = resolveInstallmentCount(product);
        BigDecimal regularPrincipal = loan.getPrincipalAmount()
                .divide(BigDecimal.valueOf(count), 4, RoundingMode.DOWN);
        BigDecimal allocatedPrincipal = BigDecimal.ZERO;
        for (int installmentNumber = 1; installmentNumber <= count; installmentNumber++) {
            LocalDate installmentDueDate = installmentDate(loan, start, dueDate, installmentNumber, count);
            // Put the rounding remainder in the last installment so principal is conserved.
            BigDecimal principalDue = installmentNumber == count
                    ? loan.getPrincipalAmount().subtract(allocatedPrincipal) : regularPrincipal;
            allocatedPrincipal = allocatedPrincipal.add(principalDue);
            loan.addInstallment(Installment.builder().installmentNumber(installmentNumber)
                    .dueDate(installmentDueDate).principalDue(principalDue).build());
        }
    }

    private int resolveInstallmentCount(Product product) {
        if (product.getLoanStructureType() == LoanStructureType.LUMP_SUM) {
            return 1;
        }
        Integer installmentCount = product.getInstallmentCount();
        if (installmentCount == null || installmentCount < 1) {
            throw new BusinessException("Installment products must define a positive installmentCount");
        }
        return installmentCount;
    }

    private LocalDate installmentDate(Loan loan, LocalDate start, LocalDate dueDate, int number, int count) {
        Product product = loan.getProduct();
        if (product.getLoanStructureType() == LoanStructureType.LUMP_SUM) {
            return dueDate;
        }
        if (product.getTenureType() == TenureType.MONTHS) {
            long monthOffset = ((long) loan.getTenureValue() * number + count - 1) / count;
            LocalDate date = start.plusMonths(monthOffset);
            // Always anchor to the original date; Jan 31 -> Feb end -> Mar 31.
            return start.getDayOfMonth() == start.lengthOfMonth()
                    ? date.withDayOfMonth(date.lengthOfMonth()) : date;
        }
        long days = ChronoUnit.DAYS.between(start, dueDate);
        return start.plusDays((days * number + count - 1) / count);
    }

    private void applyServiceFees(Loan loan, FeeApplicationTiming timing) {
        for (Fee fee : loan.getProduct().getFees()) {
            if (!fee.isActive() || fee.getFeeCategory() != FeeCategory.SERVICE_FEE
                    || fee.getApplicationTiming() != timing) {
                continue;
            }
            String key = loan.getLoanNumber() + ":" + timing.name() + ":" + fee.getId();
            if (!charges.existsByDeduplicationKey(key)) {
                loan.applyCharge(LoanCharge.builder().feeConfiguration(fee).feeCategory(fee.getFeeCategory())
                        .amount(feeCalculator.calculate(fee, loan.getPrincipalAmount()))
                        .appliedDate(LocalDate.now(clock)).deduplicationKey(key)
                        .description("Service fee applied at " + timing.name().toLowerCase(Locale.ROOT)).build());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LoanResponse getLoan(Long loanId) {
        return LoanResponse.from(findLoan(loanId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> listLoans() {
        return loans.findAll().stream().map(LoanResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanResponse> listLoansForCustomer(Long customerId) {
        return loans.findByCustomerId(customerId).stream().map(LoanResponse::from).toList();
    }

    @Override
    public RepaymentResponse repay(Long loanId, RepaymentRequest request) {
        Loan loan = findLoan(loanId);
        if (!loan.hasActiveExposure()) {
            throw new BusinessException("Cannot repay a loan in state " + loan.getState());
        }
        if (request.amount().compareTo(loan.totalOutstanding()) > 0) {
            throw new BusinessException("Repayment amount exceeds total outstanding balance of " + loan.totalOutstanding());
        }
        List<Installment> installments = loan.getInstallments().stream()
                .sorted(Comparator.comparing(Installment::getDueDate)).toList();
        BigDecimal feePayment = request.amount().min(loan.getOutstandingFees());
        allocateInstallmentFees(installments, feePayment);
        loan.setOutstandingFees(loan.getOutstandingFees().subtract(feePayment));
        BigDecimal principalPayment = allocatePrincipal(installments, request.amount().subtract(feePayment));
        loan.setOutstandingPrincipal(loan.getOutstandingPrincipal().subtract(principalPayment));
        Repayment repayment = Repayment.builder().reference(request.reference()).amount(request.amount())
                .allocatedToFees(feePayment).allocatedToPrincipal(principalPayment)
                .paidAt(Instant.now(clock)).build();
        loan.addRepayment(repayment);
        updateStateAfterRepayment(loan);
        Loan saved = loans.save(loan);
        publish(NotificationEventType.REPAYMENT_ACKNOWLEDGED, saved, Map.of("amount", request.amount().toPlainString()));
        if (saved.getState() == LoanState.CLOSED) {
            publish(NotificationEventType.LOAN_CLOSED, saved, Map.of());
        }
        return RepaymentResponse.from(saved.getRepayments().get(saved.getRepayments().size() - 1));
    }

    private void allocateInstallmentFees(List<Installment> installments, BigDecimal feePayment) {
        BigDecimal remaining = feePayment;
        for (Installment installment : installments) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal payment = remaining.min(installment.getFeesDue().subtract(installment.getFeesPaid()));
            installment.setFeesPaid(installment.getFeesPaid().add(payment));
            remaining = remaining.subtract(payment);
            installment.markPaidIfSettled();
        }
    }

    private BigDecimal allocatePrincipal(List<Installment> installments, BigDecimal principalPayment) {
        BigDecimal remaining = principalPayment;
        for (Installment installment : installments) {
            if (remaining.signum() <= 0) {
                break;
            }
            if (installment.getState() == InstallmentState.PAID) {
                continue;
            }
            BigDecimal payment = remaining.min(installment.getPrincipalDue().subtract(installment.getAmountPaid()));
            installment.setAmountPaid(installment.getAmountPaid().add(payment));
            remaining = remaining.subtract(payment);
            installment.markPaidIfSettled();
        }
        return principalPayment.subtract(remaining);
    }

    private void updateStateAfterRepayment(Loan loan) {
        if (loan.totalOutstanding().signum() <= 0) {
            loan.setState(LoanState.CLOSED);
            loan.setClosedAt(Instant.now(clock));
            releaseLimit(loan);
        } else if (loan.getInstallments().stream().anyMatch(installment -> installment.isOverdueOn(LocalDate.now(clock)))) {
            loan.setState(LoanState.OVERDUE);
        }
    }

    @Override
    public LoanResponse cancelLoan(Long loanId) {
        Loan loan = findLoan(loanId);
        if (loan.getState() != LoanState.PENDING && loan.getState() != LoanState.OPEN) {
            throw new BusinessException("Only PENDING or OPEN loans can be cancelled");
        }
        if (!loan.getRepayments().isEmpty()) {
            throw new BusinessException("Cannot cancel a loan that already has repayments recorded");
        }
        boolean limitReserved = loan.getState() == LoanState.OPEN;
        loan.setState(LoanState.CANCELLED);
        loan.setClosedAt(Instant.now(clock));
        if (limitReserved) {
            releaseLimit(loan);
        }
        return LoanResponse.from(loans.save(loan));
    }

    @Override
    public LoanResponse writeOffLoan(Long loanId) {
        Loan loan = findLoan(loanId);
        if (!loan.hasActiveExposure()) {
            throw new BusinessException("Only OPEN or OVERDUE loans can be written off");
        }
        loan.setState(LoanState.WRITTEN_OFF);
        loan.setClosedAt(Instant.now(clock));
        Loan saved = loans.save(loan);
        publish(NotificationEventType.LOAN_WRITTEN_OFF, saved, Map.of());
        return LoanResponse.from(saved);
    }

    private void releaseLimit(Loan loan) {
        findLimit(loan.getCustomer().getId()).release(loan.getPrincipalAmount(), Instant.now(clock));
    }

    private LoanLimit findLimit(Long customerId) {
        return limits.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan limit not found for customer id " + customerId));
    }

    private Loan findLoan(Long loanId) {
        return loans.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id " + loanId));
    }

    private void publish(NotificationEventType eventType, Loan loan, Map<String, String> variables) {
        events.publishEvent(new LendingNotificationEvent(eventType, loan.getCustomer(), loan, variables));
    }
}
