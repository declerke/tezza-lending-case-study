package com.example.lending.dto.loan;

import com.example.lending.domain.loan.Installment;
import com.example.lending.domain.loan.InstallmentState;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentResponse(
        Long id,
        Integer installmentNumber,
        LocalDate dueDate,
        BigDecimal principalDue,
        BigDecimal feesDue,
        BigDecimal amountPaid,
        BigDecimal feesPaid,
        InstallmentState state
) {
    public static InstallmentResponse from(Installment installment) {
        return new InstallmentResponse(
                installment.getId(),
                installment.getInstallmentNumber(),
                installment.getDueDate(),
                installment.getPrincipalDue(),
                installment.getFeesDue(),
                installment.getAmountPaid(),
                installment.getFeesPaid(),
                installment.getState()
        );
    }
}
