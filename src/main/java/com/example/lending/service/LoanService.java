package com.example.lending.service;

import com.example.lending.dto.loan.LoanCreateRequest;
import com.example.lending.dto.loan.LoanResponse;
import com.example.lending.dto.loan.RepaymentRequest;
import com.example.lending.dto.loan.RepaymentResponse;

import java.util.List;

public interface LoanService {
    LoanResponse createLoan(LoanCreateRequest request);
    LoanResponse disburseLoan(Long loanId);
    LoanResponse createAndDisburseLoan(LoanCreateRequest request);
    LoanResponse getLoan(Long id);
    List<LoanResponse> listLoans();
    List<LoanResponse> listLoansForCustomer(Long customerId);
    RepaymentResponse repay(Long loanId, RepaymentRequest request);
    LoanResponse cancelLoan(Long loanId);
    LoanResponse writeOffLoan(Long loanId);
}
