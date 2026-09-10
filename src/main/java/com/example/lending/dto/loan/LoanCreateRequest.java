package com.example.lending.dto.loan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoanCreateRequest(
        @NotNull Long customerId,
        @NotNull Long productId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal principalAmount,
        @NotNull @Min(1) Integer tenureValue
) {

    public LoanCreateRequest(Long customerId, Long productId, BigDecimal principalAmount,
                             Integer tenureValue, String ignoredConsolidatedGroupId) {
        this(customerId, productId, principalAmount, tenureValue);
    }
}
