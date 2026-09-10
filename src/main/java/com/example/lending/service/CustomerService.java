package com.example.lending.service;

import com.example.lending.dto.customer.CustomerCreateRequest;
import com.example.lending.dto.customer.CustomerResponse;
import com.example.lending.dto.customer.LoanLimitResponse;
import com.example.lending.dto.customer.LoanLimitUpdateRequest;
import com.example.lending.dto.customer.CustomerBillingUpdateRequest;
import com.example.lending.dto.customer.CustomerPreferenceUpdateRequest;
import com.example.lending.dto.customer.CustomerFinancialHistoryResponse;

import java.util.List;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerCreateRequest request);
    CustomerResponse getCustomer(Long id);
    List<CustomerResponse> listCustomers();
    LoanLimitResponse getLoanLimit(Long customerId);
    LoanLimitResponse updateLoanLimit(Long customerId, LoanLimitUpdateRequest request);
    CustomerResponse updateBillingCycle(Long customerId, CustomerBillingUpdateRequest request);
    CustomerResponse updateNotificationPreference(Long customerId, CustomerPreferenceUpdateRequest request);
    CustomerFinancialHistoryResponse getFinancialHistory(Long customerId);
}
