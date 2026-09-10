package com.example.lending.dto.customer;

import java.math.BigDecimal;

public record CustomerFinancialHistoryResponse(
        Long customerId,
        long totalLoans,
        long openLoans,
        long closedLoans,
        long overdueLoans,
        long writtenOffLoans,
        BigDecimal currentOpenExposure,
        BigDecimal totalRepaid,
        BigDecimal maxLimit,
        BigDecimal availableLimit
) {
}
