package com.example.lending.service.impl;

import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.customer.LoanLimit;
import com.example.lending.domain.loan.Loan;
import com.example.lending.domain.loan.LoanState;
import com.example.lending.domain.loan.Repayment;
import com.example.lending.domain.notification.NotificationChannel;
import com.example.lending.domain.product.BillingCycleType;
import com.example.lending.dto.customer.*;
import com.example.lending.exception.BusinessException;
import com.example.lending.exception.ResourceNotFoundException;
import com.example.lending.repository.CustomerRepository;
import com.example.lending.repository.LoanLimitRepository;
import com.example.lending.repository.LoanRepository;
import com.example.lending.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {
    private final CustomerRepository customers;
    private final LoanLimitRepository limits;
    private final LoanRepository loans;
    private final Clock clock;

    public CustomerServiceImpl(CustomerRepository customers, LoanLimitRepository limits,
                               LoanRepository loans, Clock clock) {
        this.customers = customers;
        this.limits = limits;
        this.loans = loans;
        this.clock = clock;
    }

    @Override
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        if (customers.existsByEmailOrPhoneOrNationalId(request.email(), request.phone(), request.nationalId())) {
            throw new BusinessException("A customer with the given email, phone or national ID already exists");
        }
        BillingCycleType billingCycle = Objects.requireNonNullElse(
                request.billingCycleType(), BillingCycleType.INDIVIDUAL_DUE_DATE);
        validateBilling(billingCycle, request.billingDay());
        Customer customer = Customer.builder().firstName(request.firstName()).lastName(request.lastName())
                .email(request.email()).phone(request.phone()).nationalId(request.nationalId())
                .address(request.address()).customerSegment(request.customerSegment())
                .preferredChannel(Objects.requireNonNullElse(request.preferredChannel(), NotificationChannel.EMAIL))
                .billingCycleType(billingCycle)
                .billingDay(billingCycle == BillingCycleType.CONSOLIDATED ? request.billingDay() : null)
                .createdAt(Instant.now(clock)).build();
        Customer saved = customers.save(customer);
        limits.save(LoanLimit.builder().customer(saved).maxLimit(request.initialLoanLimit())
                .availableLimit(request.initialLoanLimit()).riskTier("STANDARD")
                .updatedAt(Instant.now(clock)).build());
        return CustomerResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Long customerId) {
        return CustomerResponse.from(findCustomer(customerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> listCustomers() {
        return customers.findAll().stream().map(CustomerResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LoanLimitResponse getLoanLimit(Long customerId) {
        return LoanLimitResponse.from(findLimit(customerId));
    }

    @Override
    public LoanLimitResponse updateLoanLimit(Long customerId, LoanLimitUpdateRequest request) {
        LoanLimit loanLimit = findLimit(customerId);
        BigDecimal usedLimit = loanLimit.getMaxLimit().subtract(loanLimit.getAvailableLimit());
        if (request.maxLimit().compareTo(usedLimit) < 0) {
            throw new BusinessException("New limit cannot be lower than the amount already in use");
        }
        loanLimit.setMaxLimit(request.maxLimit());
        loanLimit.setAvailableLimit(request.maxLimit().subtract(usedLimit));
        loanLimit.setUpdatedAt(Instant.now(clock));
        return LoanLimitResponse.from(limits.save(loanLimit));
    }

    @Override
    public CustomerResponse updateBillingCycle(Long customerId, CustomerBillingUpdateRequest request) {
        Customer customer = findCustomer(customerId);
        validateBilling(request.billingCycleType(), request.billingDay());
        customer.setBillingCycleType(request.billingCycleType());
        customer.setBillingDay(request.billingCycleType() == BillingCycleType.CONSOLIDATED ? request.billingDay() : null);
        return CustomerResponse.from(customers.save(customer));
    }

    @Override
    public CustomerResponse updateNotificationPreference(Long customerId, CustomerPreferenceUpdateRequest request) {
        Customer customer = findCustomer(customerId);
        customer.setPreferredChannel(request.preferredChannel());
        return CustomerResponse.from(customers.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerFinancialHistoryResponse getFinancialHistory(Long customerId) {
        findCustomer(customerId);
        List<Loan> history = loans.findByCustomerId(customerId);
        long openLoans = history.stream().filter(Loan::hasActiveExposure).count();
        BigDecimal exposure = history.stream().filter(Loan::hasActiveExposure)
                .map(Loan::totalOutstanding).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal repaid = history.stream().flatMap(loan -> loan.getRepayments().stream())
                .map(Repayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        LoanLimit loanLimit = findLimit(customerId);
        return new CustomerFinancialHistoryResponse(customerId, history.size(), openLoans,
                countLoans(history, LoanState.CLOSED), countLoans(history, LoanState.OVERDUE),
                countLoans(history, LoanState.WRITTEN_OFF), exposure, repaid,
                loanLimit.getMaxLimit(), loanLimit.getAvailableLimit());
    }

    private long countLoans(List<Loan> history, LoanState state) {
        return history.stream().filter(loan -> loan.getState() == state).count();
    }

    private void validateBilling(BillingCycleType billingCycle, Integer billingDay) {
        if (billingCycle == BillingCycleType.CONSOLIDATED
                && (billingDay == null || billingDay < 1 || billingDay > 31)) {
            throw new BusinessException("A consolidated billing cycle requires billingDay between 1 and 31");
        }
    }

    private Customer findCustomer(Long customerId) {
        return customers.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id " + customerId));
    }

    private LoanLimit findLimit(Long customerId) {
        return limits.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan limit not found for customer id " + customerId));
    }
}
