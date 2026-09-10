package com.example.lending.dto.customer;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record LoanLimitUpdateRequest(
        @NotNull @PositiveOrZero BigDecimal maxLimit,
        String riskTier
) {
}
