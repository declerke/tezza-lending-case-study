package com.example.lending.service;

import com.example.lending.domain.product.Fee;
import com.example.lending.domain.product.FeeCalculationType;
import com.example.lending.domain.product.FeeCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FeeCalculationServiceTest {

    private final FeeCalculationService service = new FeeCalculationService();

    @Test
    void calculatesFixedFeeRegardlessOfBaseAmount() {
        Fee fee = Fee.builder()
                .feeCategory(FeeCategory.SERVICE_FEE)
                .calculationType(FeeCalculationType.FIXED)
                .amount(new BigDecimal("500"))
                .build();

        BigDecimal result = service.calculate(fee, new BigDecimal("10000"));

        assertThat(result).isEqualByComparingTo("500.0000");
    }

    @Test
    void calculatesPercentageFeeAgainstBaseAmount() {
        Fee fee = Fee.builder()
                .feeCategory(FeeCategory.SERVICE_FEE)
                .calculationType(FeeCalculationType.PERCENTAGE)
                .amount(new BigDecimal("2.5"))
                .build();

        BigDecimal result = service.calculate(fee, new BigDecimal("10000"));

        assertThat(result).isEqualByComparingTo("250.0000");
    }
}
