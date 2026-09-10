package com.example.lending.dto.customer;

import com.example.lending.domain.customer.LoanLimit;

import java.math.BigDecimal;
import java.time.Instant;

public record LoanLimitResponse(
        Long id,
        Long customerId,
        BigDecimal maxLimit,
        BigDecimal availableLimit,
        String riskTier,
        Instant updatedAt
) {
    public static LoanLimitResponse from(LoanLimit limit) {
        return new LoanLimitResponse(
                limit.getId(),
                limit.getCustomer().getId(),
                limit.getMaxLimit(),
                limit.getAvailableLimit(),
                limit.getRiskTier(),
                limit.getUpdatedAt()
        );
    }
}
