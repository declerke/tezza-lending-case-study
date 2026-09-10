package com.example.lending.dto.product;

import com.example.lending.domain.product.BillingCycleType;
import com.example.lending.domain.product.LoanStructureType;
import com.example.lending.domain.product.TenureType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ProductCreateRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotNull TenureType tenureType,
        @NotNull @Min(1) Integer minTenureValue,
        @NotNull @Min(1) Integer maxTenureValue,
        @NotNull BigDecimal interestRate,
        @NotNull LoanStructureType loanStructureType,
        @Min(1) Integer installmentCount,
        @NotNull BillingCycleType billingCycleType,
        @Valid List<FeeRequest> fees
) {
}
