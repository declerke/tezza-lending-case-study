package com.example.lending.service;

import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.customer.LoanLimit;
import com.example.lending.domain.loan.Loan;
import com.example.lending.domain.loan.LoanState;
import com.example.lending.domain.notification.NotificationEventType;
import com.example.lending.domain.product.*;
import com.example.lending.dto.loan.LoanCreateRequest;
import com.example.lending.dto.loan.LoanResponse;
import com.example.lending.dto.loan.RepaymentRequest;
import com.example.lending.dto.loan.RepaymentResponse;
import com.example.lending.exception.BusinessException;
import com.example.lending.event.LendingNotificationEvent;
import com.example.lending.repository.*;
import com.example.lending.service.impl.LoanServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private LoanLimitRepository loanLimitRepository;
    @Mock
    private LoanChargeRepository loanChargeRepository;
    @Mock
    private ApplicationEventPublisher events;

    private LoanServiceImpl loanService;
    private final FeeCalculationService feeCalculationService = new FeeCalculationService();

    private Customer customer;
    private Product lumpSumProduct;
    private LoanLimit loanLimit;

    @BeforeEach
    void setUp() {
        loanService = new LoanServiceImpl(loanRepository, productRepository, customerRepository,
                loanLimitRepository, loanChargeRepository, feeCalculationService, events,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));

        customer = Customer.builder().id(1L).firstName("Amina").lastName("Otieno")
                .email("amina@example.com").customerSegment("RETAIL").build();

        Fee serviceFee = Fee.builder().feeCategory(FeeCategory.SERVICE_FEE)
                .calculationType(FeeCalculationType.PERCENTAGE).amount(new BigDecimal("2.5")).active(true).build();

        lumpSumProduct = Product.builder().id(1L).code("QUICK-30").name("Quick 30")
                .tenureType(TenureType.DAYS).minTenureValue(7).maxTenureValue(30)
                .interestRate(new BigDecimal("5.0")).loanStructureType(LoanStructureType.LUMP_SUM)
                .billingCycleType(BillingCycleType.INDIVIDUAL_DUE_DATE).active(true)
                .fees(new java.util.ArrayList<>(List.of(serviceFee))).build();
        serviceFee.setProduct(lumpSumProduct);

        loanLimit = LoanLimit.builder().id(1L).customer(customer)
                .maxLimit(new BigDecimal("100000")).availableLimit(new BigDecimal("100000")).build();

    }

    @Test
    void createsAndDisbursesLumpSumLoanWithServiceFee() {
        stubLoanSave();
        LoanCreateRequest request = new LoanCreateRequest(1L, 1L, new BigDecimal("10000"), 30, null);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(lumpSumProduct));
        when(loanLimitRepository.findByCustomerId(1L)).thenReturn(Optional.of(loanLimit));

        LoanResponse response = loanService.createAndDisburseLoan(request);

        assertThat(response.state()).isEqualTo(LoanState.OPEN);
        assertThat(response.installments()).hasSize(1);
        assertThat(response.installments().get(0).principalDue()).isEqualByComparingTo("10000");
        assertThat(loanLimit.getAvailableLimit()).isEqualByComparingTo("90000");

        var eventCaptor = ArgumentCaptor.forClass(LendingNotificationEvent.class);
        verify(events, times(2)).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).extracting(LendingNotificationEvent::type)
                .containsExactly(NotificationEventType.LOAN_CREATED, NotificationEventType.LOAN_DISBURSED);
    }

    @Test
    void rejectsLoanWhenPrincipalExceedsAvailableLimit() {
        loanLimit.setAvailableLimit(new BigDecimal("5000"));
        LoanCreateRequest request = new LoanCreateRequest(1L, 1L, new BigDecimal("10000"), 30, null);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(lumpSumProduct));
        when(loanLimitRepository.findByCustomerId(1L)).thenReturn(Optional.of(loanLimit));

        assertThatThrownBy(() -> loanService.createAndDisburseLoan(request))
                .isInstanceOf(BusinessException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    void rejectsTenureOutsideProductRange() {
        LoanCreateRequest request = new LoanCreateRequest(1L, 1L, new BigDecimal("10000"), 90, null);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(lumpSumProduct));

        assertThatThrownBy(() -> loanService.createAndDisburseLoan(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void repaymentAllocatesToFeesBeforePrincipalAndClosesFullyPaidLoan() {
        stubLoanSave();
        LoanCreateRequest request = new LoanCreateRequest(1L, 1L, new BigDecimal("10000"), 30, null);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(lumpSumProduct));
        when(loanLimitRepository.findByCustomerId(1L)).thenReturn(Optional.of(loanLimit));

        loanService.createAndDisburseLoan(request);

        // Fetch the persisted loan captured by the save() stub for use in repay()
        Loan persisted = captureLastSavedLoan();
        when(loanRepository.findById(persisted.getId())).thenReturn(Optional.of(persisted));

        assertThat(persisted.getOutstandingFees()).isEqualByComparingTo("250.0000");
        assertThat(persisted.totalOutstanding()).isEqualByComparingTo("10250.0000");

        RepaymentRequest repaymentRequest = new RepaymentRequest(new BigDecimal("10250.0000"), "REF-1");
        RepaymentResponse repaymentResponse = loanService.repay(persisted.getId(), repaymentRequest);

        assertThat(repaymentResponse.allocatedToFees()).isEqualByComparingTo("250.0000");
        assertThat(repaymentResponse.allocatedToPrincipal()).isEqualByComparingTo("10000.0000");
        assertThat(repaymentResponse.loanState()).isEqualTo(LoanState.CLOSED);
        assertThat(loanLimit.getAvailableLimit()).isEqualByComparingTo("100000");
    }

    @Test
    void rejectsRepaymentExceedingOutstandingBalance() {
        stubLoanSave();
        LoanCreateRequest request = new LoanCreateRequest(1L, 1L, new BigDecimal("10000"), 30, null);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(1L)).thenReturn(Optional.of(lumpSumProduct));
        when(loanLimitRepository.findByCustomerId(1L)).thenReturn(Optional.of(loanLimit));

        loanService.createAndDisburseLoan(request);
        Loan persisted = captureLastSavedLoan();
        when(loanRepository.findById(persisted.getId())).thenReturn(Optional.of(persisted));

        RepaymentRequest repaymentRequest = new RepaymentRequest(new BigDecimal("999999"), "REF-1");

        assertThatThrownBy(() -> loanService.repay(persisted.getId(), repaymentRequest))
                .isInstanceOf(BusinessException.class);
    }

    private Loan captureLastSavedLoan() {
        var captor = org.mockito.ArgumentCaptor.forClass(Loan.class);
        verify(loanRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private void stubLoanSave() {
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan loan = inv.getArgument(0);
            if (loan.getId() == null) {
                loan.setId(1L);
            }
            return loan;
        });
    }
}
