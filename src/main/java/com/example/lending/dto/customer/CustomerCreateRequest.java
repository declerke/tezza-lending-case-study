package com.example.lending.dto.customer;

import com.example.lending.domain.notification.NotificationChannel;
import com.example.lending.domain.product.BillingCycleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CustomerCreateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotBlank String nationalId,
        String address,
        String customerSegment,
        NotificationChannel preferredChannel,
        @NotNull @PositiveOrZero BigDecimal initialLoanLimit,
        BillingCycleType billingCycleType,
        Integer billingDay
) {

    public CustomerCreateRequest(String firstName, String lastName, String email, String phone,
                                 String nationalId, String address, String customerSegment,
                                 NotificationChannel preferredChannel, BigDecimal initialLoanLimit) {
        this(firstName, lastName, email, phone, nationalId, address, customerSegment,
                preferredChannel, initialLoanLimit, BillingCycleType.INDIVIDUAL_DUE_DATE, null);
    }
}
