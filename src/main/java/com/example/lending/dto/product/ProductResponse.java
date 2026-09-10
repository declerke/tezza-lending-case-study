package com.example.lending.dto.product;

import com.example.lending.domain.product.BillingCycleType;
import com.example.lending.domain.product.LoanStructureType;
import com.example.lending.domain.product.Product;
import com.example.lending.domain.product.TenureType;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        String code,
        String name,
        String description,
        TenureType tenureType,
        Integer minTenureValue,
        Integer maxTenureValue,
        BigDecimal interestRate,
        LoanStructureType loanStructureType,
        Integer installmentCount,
        BillingCycleType billingCycleType,
        boolean active,
        List<FeeResponse> fees
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getDescription(),
                product.getTenureType(),
                product.getMinTenureValue(),
                product.getMaxTenureValue(),
                product.getInterestRate(),
                product.getLoanStructureType(),
                product.getInstallmentCount(),
                product.getBillingCycleType(),
                product.isActive(),
                product.getFees().stream().map(FeeResponse::from).toList()
        );
    }
}
