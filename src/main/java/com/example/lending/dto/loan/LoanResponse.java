package com.example.lending.dto.loan;

import com.example.lending.domain.loan.Loan;
import com.example.lending.domain.loan.LoanState;
import com.example.lending.domain.product.BillingCycleType;
import com.example.lending.domain.product.LoanStructureType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LoanResponse(
        Long id,
        String loanNumber,
        Long customerId,
        Long productId,
        BigDecimal principalAmount,
        BigDecimal interestRate,
        Integer tenureValue,
        LoanStructureType loanStructureType,
        BillingCycleType billingCycleType,
        LoanState state,
        LocalDate disbursementDate,
        LocalDate dueDate,
        BigDecimal outstandingPrincipal,
        BigDecimal outstandingFees,
        List<InstallmentResponse> installments
) {
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getLoanNumber(),
                loan.getCustomer().getId(),
                loan.getProduct().getId(),
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getTenureValue(),
                loan.getLoanStructureType(),
                loan.getBillingCycleType(),
                loan.getState(),
                loan.getDisbursementDate(),
                loan.getDueDate(),
                loan.getOutstandingPrincipal(),
                loan.getOutstandingFees(),
                loan.getInstallments().stream().map(InstallmentResponse::from).toList()
        );
    }
}
