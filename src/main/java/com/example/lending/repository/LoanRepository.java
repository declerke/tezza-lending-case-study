package com.example.lending.repository;

import com.example.lending.domain.loan.Loan;
import com.example.lending.domain.loan.LoanState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findByLoanNumber(String loanNumber);
    List<Loan> findByCustomerId(Long customerId);
    List<Loan> findByState(LoanState state);
    List<Loan> findByStateIn(Collection<LoanState> states);
}
