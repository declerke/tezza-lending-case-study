package com.example.lending.dto.loan;

import com.example.lending.domain.loan.LoanState;
import com.example.lending.domain.loan.Repayment;

import java.math.BigDecimal;
import java.time.Instant;

public record RepaymentResponse(
        Long id,
        Long loanId,
        String reference,
        BigDecimal amount,
        BigDecimal allocatedToFees,
        BigDecimal allocatedToPrincipal,
        Instant paidAt,
        LoanState loanState,
        BigDecimal outstandingPrincipal,
        BigDecimal outstandingFees
) {
    public static RepaymentResponse from(Repayment repayment) {
        return new RepaymentResponse(
                repayment.getId(),
                repayment.getLoan().getId(),
                repayment.getReference(),
                repayment.getAmount(),
                repayment.getAllocatedToFees(),
                repayment.getAllocatedToPrincipal(),
                repayment.getPaidAt(),
                repayment.getLoan().getState(),
                repayment.getLoan().getOutstandingPrincipal(),
                repayment.getLoan().getOutstandingFees()
        );
    }
}
