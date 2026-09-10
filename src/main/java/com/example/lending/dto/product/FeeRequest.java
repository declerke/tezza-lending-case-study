package com.example.lending.dto.product;

import com.example.lending.domain.product.FeeCalculationType;
import com.example.lending.domain.product.FeeCategory;
import com.example.lending.domain.product.FeeApplicationTiming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record FeeRequest(
        @NotNull FeeCategory feeCategory,
        @NotNull FeeCalculationType calculationType,
        @NotNull @PositiveOrZero BigDecimal amount,
        @PositiveOrZero Integer daysAfterDue,
        @NotNull FeeApplicationTiming applicationTiming
) {

    public FeeRequest(FeeCategory feeCategory, FeeCalculationType calculationType,
                      BigDecimal amount, Integer daysAfterDue) {
        this(feeCategory, calculationType, amount, daysAfterDue, FeeApplicationTiming.ORIGINATION);
    }
}
