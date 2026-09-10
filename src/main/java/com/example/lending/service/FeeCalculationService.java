package com.example.lending.service;

import com.example.lending.domain.product.Fee;
import com.example.lending.domain.product.FeeCalculationType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FeeCalculationService {

    public BigDecimal calculate(Fee fee, BigDecimal baseAmount) {
        if (fee.getCalculationType() == FeeCalculationType.FIXED) {
            return fee.getAmount().setScale(4, RoundingMode.HALF_UP);
        }
        return baseAmount
                .multiply(fee.getAmount())
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }
}
