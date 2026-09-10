package com.example.lending.controller;

import com.example.lending.domain.loan.LoanState;
import com.example.lending.domain.notification.NotificationChannel;
import com.example.lending.domain.product.BillingCycleType;
import com.example.lending.domain.product.LoanStructureType;
import com.example.lending.dto.customer.CustomerResponse;
import com.example.lending.dto.customer.CustomerFinancialHistoryResponse;
import com.example.lending.dto.loan.LoanResponse;
import com.example.lending.dto.loan.RepaymentResponse;
import com.example.lending.exception.BusinessException;
import com.example.lending.exception.GlobalExceptionHandler;
import com.example.lending.exception.ResourceNotFoundException;
import com.example.lending.service.CustomerService;
import com.example.lending.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({CustomerController.class, LoanController.class})
@Import(GlobalExceptionHandler.class)
class LendingControllerTest {
    @Autowired MockMvc mvc;
    @MockBean CustomerService customers;
    @MockBean LoanService loans;

    @Test
    void exposesCustomerBillingAndFinancialHistoryContracts() throws Exception {
        CustomerResponse customer = customer();
        when(customers.createCustomer(any())).thenReturn(customer);
        when(customers.updateBillingCycle(eq(1L), any())).thenReturn(customer);
        when(customers.getFinancialHistory(1L)).thenReturn(new CustomerFinancialHistoryResponse(
                1L, 2, 1, 1, 0, 0, new BigDecimal("1000"), new BigDecimal("500"),
                new BigDecimal("10000"), new BigDecimal("9000")));

        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"firstName\":\"Amina\",\"lastName\":\"Otieno\",\"email\":\"amina@test.com\",\"phone\":\"0700\",\"nationalId\":\"ID-1\",\"initialLoanLimit\":10000}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(put("/api/customers/1/billing-cycle").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"billingCycleType\":\"CONSOLIDATED\",\"billingDay\":31}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.billingCycleType").value("CONSOLIDATED"));
        mvc.perform(get("/api/customers/1/financial-history"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalLoans").value(2));
    }

    @Test
    void exposesLoanCreationDisbursementAndRepaymentContracts() throws Exception {
        LoanResponse pending = loan(LoanState.PENDING);
        LoanResponse open = loan(LoanState.OPEN);
        RepaymentResponse repayment = new RepaymentResponse(5L, 4L, "R-1", new BigDecimal("100"),
                new BigDecimal("10"), new BigDecimal("90"), Instant.parse("2026-01-01T00:00:00Z"), LoanState.OPEN,
                new BigDecimal("910"), BigDecimal.ZERO);
        when(loans.createLoan(any())).thenReturn(pending);
        when(loans.disburseLoan(4L)).thenReturn(open);
        when(loans.repay(eq(4L), any())).thenReturn(repayment);

        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"customerId\":1,\"productId\":1,\"principalAmount\":1000,\"tenureValue\":30}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("PENDING"));
        mvc.perform(post("/api/loans/4/disburse"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("OPEN"));
        mvc.perform(post("/api/loans/4/repayments").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"amount\":100,\"reference\":\"R-1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.loanState").value("OPEN"));
    }

    @Test
    void returnsStableErrorsForValidationNotFoundBusinessAndConflict() throws Exception {
        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Validation failed"));
        when(customers.getCustomer(999L)).thenThrow(new ResourceNotFoundException("Customer not found"));
        mvc.perform(get("/api/customers/999")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
        when(loans.repay(eq(4L), any())).thenThrow(new BusinessException("Cannot repay a pending loan"));
        mvc.perform(post("/api/loans/4/repayments").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"amount\":100,\"reference\":\"R-1\"}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.status").value(422));
        when(loans.disburseLoan(4L)).thenThrow(new OptimisticLockingFailureException("stale"));
        mvc.perform(post("/api/loans/4/disburse")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Resource was changed concurrently; retry the operation"));
    }

    private CustomerResponse customer() {
        return new CustomerResponse(1L, "Amina", "Otieno", "amina@test.com", "0700", "ID-1", "Nairobi",
                "RETAIL", NotificationChannel.EMAIL, BillingCycleType.CONSOLIDATED, 31, true);
    }

    @Test
    void malformedJsonAndInvalidIdsReturn400WithoutInternalDetails() throws Exception {
        mvc.perform(post("/api/loans").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request contains malformed JSON or an invalid value"));
        mvc.perform(get("/api/loans/not-a-number"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void rejectsNegativeLendingLimitBeforePersistence() throws Exception {
        mvc.perform(put("/api/customers/1/loan-limit").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxLimit\":-1}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Validation failed"));
    }

    private LoanResponse loan(LoanState state) {
        return new LoanResponse(4L, "LN-4", 1L, 1L, new BigDecimal("1000"), BigDecimal.ZERO, 30,
                LoanStructureType.LUMP_SUM, BillingCycleType.INDIVIDUAL_DUE_DATE, state, null, null,
                new BigDecimal("1000"), BigDecimal.ZERO, List.of());
    }
}
