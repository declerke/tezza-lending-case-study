package com.example.lending.repository;

import com.example.lending.domain.customer.LoanLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanLimitRepository extends JpaRepository<LoanLimit, Long> {
    Optional<LoanLimit> findByCustomerId(Long customerId);
}
