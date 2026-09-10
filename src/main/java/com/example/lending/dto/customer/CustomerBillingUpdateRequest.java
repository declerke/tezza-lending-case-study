package com.example.lending.dto.customer;

import com.example.lending.domain.product.BillingCycleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CustomerBillingUpdateRequest(
        @NotNull BillingCycleType billingCycleType,
        @Min(1) @Max(31) Integer billingDay
) {
}
