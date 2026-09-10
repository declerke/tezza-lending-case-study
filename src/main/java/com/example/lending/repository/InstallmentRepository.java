package com.example.lending.repository;

import com.example.lending.domain.loan.Installment;
import com.example.lending.domain.loan.InstallmentState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {
    List<Installment> findByLoanId(Long loanId);
    List<Installment> findByStateAndDueDateBefore(InstallmentState state, LocalDate date);
}
