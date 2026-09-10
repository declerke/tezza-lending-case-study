package com.example.lending.repository;

import com.example.lending.domain.customer.Customer;
import com.example.lending.domain.customer.LoanLimit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class LoanLimitConcurrencyTest {
    @Autowired LoanLimitRepository limits;
    @Autowired CustomerRepository customers;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectsConcurrentLimitReservationUsingOptimisticLocking() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        LoanLimit created = Objects.requireNonNull(transaction.execute(status -> {
            Customer customer = customers.save(Customer.builder().firstName("Lock").lastName("Test")
                    .email("locking@test.invalid").phone("LOCK-TEST").nationalId("LOCK-TEST").build());
            return limits.saveAndFlush(LoanLimit.builder().customer(customer).maxLimit(new BigDecimal("100000"))
                    .availableLimit(new BigDecimal("100000")).build());
        }));
        try {
            LoanLimit first = Objects.requireNonNull(transaction.execute(status -> limits.findById(created.getId()).orElseThrow()));
            LoanLimit second = Objects.requireNonNull(transaction.execute(status -> limits.findById(created.getId()).orElseThrow()));
            Instant reservedAt = Instant.parse("2026-01-01T00:00:00Z");
            first.reserve(new BigDecimal("60000"), reservedAt);
            second.reserve(new BigDecimal("60000"), reservedAt);

            transaction.executeWithoutResult(status -> limits.saveAndFlush(first));

            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> limits.saveAndFlush(second)))
                    .isInstanceOf(OptimisticLockingFailureException.class);
            LoanLimit committed = Objects.requireNonNull(transaction.execute(status -> limits.findById(created.getId()).orElseThrow()));
            assertThat(committed.getAvailableLimit()).isEqualByComparingTo("40000");
        } finally {
            transaction.executeWithoutResult(status -> {
                limits.deleteById(created.getId());
                limits.flush();
                customers.deleteById(created.getCustomer().getId());
            });
        }
    }
}
