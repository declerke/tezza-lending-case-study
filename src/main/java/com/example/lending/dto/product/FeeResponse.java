package com.example.lending.dto.product;

import com.example.lending.domain.product.Fee;
import com.example.lending.domain.product.FeeCalculationType;
import com.example.lending.domain.product.FeeCategory;
import com.example.lending.domain.product.FeeApplicationTiming;

import java.math.BigDecimal;

public record FeeResponse(
        Long id,
        FeeCategory feeCategory,
        FeeCalculationType calculationType,
        BigDecimal amount,
        Integer daysAfterDue,
        FeeApplicationTiming applicationTiming,
        boolean active
) {
    public static FeeResponse from(Fee fee) {
        return new FeeResponse(
                fee.getId(),
                fee.getFeeCategory(),
                fee.getCalculationType(),
                fee.getAmount(),
                fee.getDaysAfterDue(),
                fee.getApplicationTiming(),
                fee.isActive()
        );
    }
}
