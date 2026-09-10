package com.example.lending.dto.customer;

import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.notification.NotificationChannel;
import com.example.lending.domain.product.BillingCycleType;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String nationalId,
        String address,
        String customerSegment,
        NotificationChannel preferredChannel,
        BillingCycleType billingCycleType,
        Integer billingDay,
        boolean active
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getNationalId(),
                customer.getAddress(),
                customer.getCustomerSegment(),
                customer.getPreferredChannel(),
                customer.getBillingCycleType(),
                customer.getBillingDay(),
                customer.isActive()
        );
    }
}
