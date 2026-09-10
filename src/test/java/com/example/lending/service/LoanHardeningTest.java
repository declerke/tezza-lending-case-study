package com.example.lending.service;

import com.example.lending.domain.customer.*;
import com.example.lending.domain.loan.*;
import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.domain.product.*;
import com.example.lending.dto.loan.*;
import com.example.lending.event.LendingNotificationEvent;
import com.example.lending.exception.BusinessException;
import com.example.lending.repository.*;
import com.example.lending.service.impl.LoanServiceImpl;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanHardeningTest {
    @Mock LoanRepository loans;
    @Mock ProductRepository products;
    @Mock CustomerRepository customers;
    @Mock LoanLimitRepository limits;
    @Mock LoanChargeRepository charges;
    @Mock ApplicationEventPublisher events;
    private LoanServiceImpl service;
    private Customer customer;
    private Product product;
    private LoanLimit limit;
    private Loan savedLoan;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(1L).firstName("A").lastName("B").email("a@b.test")
                .billingCycleType(BillingCycleType.INDIVIDUAL_DUE_DATE).build();
        product = Product.builder().id(2L).code("M").name("Monthly").tenureType(TenureType.MONTHS)
                .minTenureValue(3).maxTenureValue(6).interestRate(BigDecimal.ZERO)
                .loanStructureType(LoanStructureType.INSTALLMENTS).installmentCount(3)
                .billingCycleType(BillingCycleType.INDIVIDUAL_DUE_DATE).fees(new ArrayList<>()).build();
        limit = LoanLimit.builder().customer(customer).maxLimit(new BigDecimal("100000"))
                .availableLimit(new BigDecimal("100000")).build();
        service = serviceOn(LocalDate.of(2026, 1, 31));
    }

    private LoanServiceImpl serviceOn(LocalDate date) {
        return new LoanServiceImpl(loans, products, customers, limits, charges, new FeeCalculationService(),
                events, Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
    }

    private Loan originate(int tenure) {
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(products.findById(2L)).thenReturn(Optional.of(product));
        when(limits.findByCustomerId(1L)).thenReturn(Optional.of(limit));
        when(loans.save(any(Loan.class))).thenAnswer(invocation -> {
            savedLoan = invocation.getArgument(0);
            if (savedLoan.getId() == null) savedLoan.setId(1L);
            return savedLoan;
        });
        service.createLoan(new LoanCreateRequest(1L, 2L, new BigDecimal("9000"), tenure));
        return savedLoan;
    }

    private LoanResponse disburse(Loan loan) {
        when(loans.findById(loan.getId())).thenReturn(Optional.of(loan));
        return service.disburseLoan(loan.getId());
    }

    @Test
    void creationIsPendingUntilExplicitDisbursement() {
        Loan pending = originate(3);

        assertThat(pending.getState()).isEqualTo(LoanState.PENDING);
        assertThat(pending.getDisbursementDate()).isNull();
        ArgumentCaptor<LendingNotificationEvent> event = ArgumentCaptor.forClass(LendingNotificationEvent.class);
        verify(events).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo(NotificationEventType.LOAN_CREATED);
    }

    @Test
    void monthlyScheduleUsesCalendarProgression() {
        LoanResponse response = disburse(originate(3));

        assertThat(response.installments()).extracting(InstallmentResponse::dueDate)
                .containsExactly(LocalDate.of(2026, 2, 28), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 30));
    }

    @Test
    void pendingLoanCanBeCancelled() {
        Loan pending = originate(3);
        when(loans.findById(1L)).thenReturn(Optional.of(pending));

        assertThat(service.cancelLoan(1L).state()).isEqualTo(LoanState.CANCELLED);
        assertThat(limit.getAvailableLimit()).isEqualByComparingTo("100000");
    }

    @Test
    void appliesPostDisbursementFeeOnlyAfterDisbursement() {
        product.getFees().addAll(List.of(serviceFee(10L, "100", FeeApplicationTiming.ORIGINATION),
                serviceFee(11L, "200", FeeApplicationTiming.POST_DISBURSEMENT)));
        Loan pending = originate(3);
        assertThat(pending.getOutstandingFees()).isEqualByComparingTo("100");

        assertThat(disburse(pending).outstandingFees()).isEqualByComparingTo("300");
        assertThatThrownBy(() -> service.disburseLoan(1L)).isInstanceOf(BusinessException.class);
        assertThat(pending.getCharges()).hasSize(2);
    }

    @Test
    void consolidatedCustomerLoansShareDueDate() {
        useConsolidatedDaysProduct();
        Loan first = originate(10);
        assertThat(disburse(first).dueDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        when(loans.findByCustomerId(1L)).thenReturn(List.of(first));

        Loan second = originate(20);

        assertThat(disburse(second).dueDate()).isEqualTo(first.getDueDate());
    }

    @ParameterizedTest
    @CsvSource({"2026-01-31,2026-02-28", "2026-04-01,2026-04-30", "2026-03-01,2026-03-31", "2028-01-31,2028-02-29"})
    void clampsConsolidatedBillingDay31ToMonthEnd(LocalDate start, LocalDate expectedDueDate) {
        useConsolidatedDaysProduct();
        service = serviceOn(start);

        assertThat(disburse(originate(10)).dueDate()).isEqualTo(expectedDueDate);
    }

    @Test
    void settlesInstallmentWhenRepaymentClearsOnlyRemainingFees() {
        Loan loan = originate(3);
        disburse(loan);
        loan.setOutstandingPrincipal(BigDecimal.ZERO);
        loan.setOutstandingFees(new BigDecimal("100"));
        loan.getInstallments().forEach(installment -> installment.setAmountPaid(installment.getPrincipalDue()));
        Installment overdue = loan.getInstallments().get(0);
        overdue.setState(InstallmentState.OVERDUE);
        overdue.setFeesDue(new BigDecimal("100"));

        RepaymentResponse response = service.repay(loan.getId(), new RepaymentRequest(new BigDecimal("100"), "FEE-ONLY"));

        assertThat(overdue.getState()).isEqualTo(InstallmentState.PAID);
        assertThat(response.loanState()).isEqualTo(LoanState.CLOSED);
        assertThat(response.outstandingFees()).isEqualByComparingTo("0");
        assertThat(limit.getAvailableLimit()).isEqualByComparingTo("100000");
    }

    private void useConsolidatedDaysProduct() {
        customer.setBillingCycleType(BillingCycleType.CONSOLIDATED);
        customer.setBillingDay(31);
        product.setTenureType(TenureType.DAYS);
        product.setMinTenureValue(1);
        product.setMaxTenureValue(40);
        product.setLoanStructureType(LoanStructureType.LUMP_SUM);
    }

    private Fee serviceFee(Long id, String amount, FeeApplicationTiming timing) {
        return Fee.builder().id(id).feeCategory(FeeCategory.SERVICE_FEE).calculationType(FeeCalculationType.FIXED)
                .applicationTiming(timing).amount(new BigDecimal(amount)).active(true).build();
    }
}
