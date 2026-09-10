package com.example.lending.service;

import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.customer.LoanLimit;
import com.example.lending.dto.customer.CustomerCreateRequest;
import com.example.lending.dto.customer.CustomerResponse;
import com.example.lending.dto.customer.LoanLimitUpdateRequest;
import com.example.lending.exception.BusinessException;
import com.example.lending.repository.CustomerRepository;
import com.example.lending.repository.LoanLimitRepository;
import com.example.lending.repository.LoanRepository;
import com.example.lending.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LoanLimitRepository loanLimitRepository;

    @Mock
    private LoanRepository loanRepository;

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository, loanLimitRepository, loanRepository,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void createsCustomerWithInitialLoanLimit() {
        CustomerCreateRequest request = new CustomerCreateRequest(
                "Amina", "Otieno", "amina@example.com", "+254700000000", "ID-1",
                "Nairobi", "RETAIL", null, new BigDecimal("50000"));

        when(customerRepository.existsByEmailOrPhoneOrNationalId(any(), any(), any())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(loanLimitRepository.save(any(LoanLimit.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = customerService.createCustomer(request);

        assertThat(response.id()).isEqualTo(1L);
        verify(loanLimitRepository).save(any(LoanLimit.class));
    }

    @Test
    void rejectsDuplicateCustomer() {
        CustomerCreateRequest request = new CustomerCreateRequest(
                "Amina", "Otieno", "amina@example.com", "+254700000000", "ID-1",
                "Nairobi", "RETAIL", null, new BigDecimal("50000"));

        when(customerRepository.existsByEmailOrPhoneOrNationalId(any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsLoanLimitReductionBelowUsedAmount() {
        Customer customer = Customer.builder().id(1L).build();
        LoanLimit existing = LoanLimit.builder()
                .id(1L)
                .customer(customer)
                .maxLimit(new BigDecimal("100000"))
                .availableLimit(new BigDecimal("20000"))
                .build();

        when(loanLimitRepository.findByCustomerId(1L)).thenReturn(Optional.of(existing));

        LoanLimitUpdateRequest request = new LoanLimitUpdateRequest(new BigDecimal("50000"), null);

        assertThatThrownBy(() -> customerService.updateLoanLimit(1L, request))
                .isInstanceOf(BusinessException.class);
    }
}
