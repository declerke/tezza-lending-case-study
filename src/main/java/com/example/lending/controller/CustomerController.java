package com.example.lending.controller;

import com.example.lending.dto.customer.CustomerCreateRequest;
import com.example.lending.dto.customer.CustomerResponse;
import com.example.lending.dto.customer.LoanLimitResponse;
import com.example.lending.dto.customer.LoanLimitUpdateRequest;
import com.example.lending.dto.customer.CustomerBillingUpdateRequest;
import com.example.lending.dto.customer.CustomerPreferenceUpdateRequest;
import com.example.lending.dto.customer.CustomerFinancialHistoryResponse;
import com.example.lending.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request));
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) {
        return customerService.getCustomer(id);
    }

    @GetMapping
    public List<CustomerResponse> list() {
        return customerService.listCustomers();
    }

    @GetMapping("/{id}/loan-limit")
    public LoanLimitResponse getLoanLimit(@PathVariable Long id) {
        return customerService.getLoanLimit(id);
    }

    @PutMapping("/{id}/loan-limit")
    public LoanLimitResponse updateLoanLimit(@PathVariable Long id, @Valid @RequestBody LoanLimitUpdateRequest request) {
        return customerService.updateLoanLimit(id, request);
    }

    @PutMapping("/{id}/billing-cycle")
    public CustomerResponse updateBilling(@PathVariable Long id, @Valid @RequestBody CustomerBillingUpdateRequest request) { return customerService.updateBillingCycle(id, request); }

    @PutMapping("/{id}/notification-preference")
    public CustomerResponse updatePreference(@PathVariable Long id, @Valid @RequestBody CustomerPreferenceUpdateRequest request) { return customerService.updateNotificationPreference(id, request); }

    @GetMapping("/{id}/financial-history")
    public CustomerFinancialHistoryResponse financialHistory(@PathVariable Long id) { return customerService.getFinancialHistory(id); }
}
