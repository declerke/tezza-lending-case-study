package com.example.lending.controller;

import com.example.lending.dto.loan.LoanCreateRequest;
import com.example.lending.dto.loan.LoanResponse;
import com.example.lending.dto.loan.RepaymentRequest;
import com.example.lending.dto.loan.RepaymentResponse;
import com.example.lending.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping
    public ResponseEntity<LoanResponse> create(@Valid @RequestBody LoanCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.createLoan(request));
    }

    @PostMapping("/{id}/disburse")
    public LoanResponse disburse(@PathVariable Long id) { return loanService.disburseLoan(id); }

    @GetMapping("/{id}")
    public LoanResponse get(@PathVariable Long id) {
        return loanService.getLoan(id);
    }

    @GetMapping
    public List<LoanResponse> list(@RequestParam(required = false) Long customerId) {
        return customerId != null ? loanService.listLoansForCustomer(customerId) : loanService.listLoans();
    }

    @PostMapping("/{id}/repayments")
    public RepaymentResponse repay(@PathVariable Long id, @Valid @RequestBody RepaymentRequest request) {
        return loanService.repay(id, request);
    }

    @PostMapping("/{id}/cancel")
    public LoanResponse cancel(@PathVariable Long id) {
        return loanService.cancelLoan(id);
    }

    @PostMapping("/{id}/write-off")
    public LoanResponse writeOff(@PathVariable Long id) {
        return loanService.writeOffLoan(id);
    }
}
