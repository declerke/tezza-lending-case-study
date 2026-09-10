package com.example.lending.dto.loan;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RepaymentRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String reference
) {
}
