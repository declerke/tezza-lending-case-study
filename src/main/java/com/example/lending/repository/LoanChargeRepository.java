package com.example.lending.repository;

import com.example.lending.domain.loan.LoanCharge;
import com.example.lending.domain.product.FeeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface LoanChargeRepository extends JpaRepository<LoanCharge, Long> {
    boolean existsByLoanIdAndFeeCategoryAndAppliedDate(Long loanId, FeeCategory feeCategory, LocalDate appliedDate);
    boolean existsByDeduplicationKey(String deduplicationKey);
}
